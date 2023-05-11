interface Visitor {

    /**
     * Visits JSON Leaf elements.
     */
    fun visit(jsonLeaf: JsonLeaf<*>) {}

    /**
     * Visits JSON Composite elements.
     */
    fun visit(jsonComposite: JsonComposite) {}

    /**
     * Visits JSON Elements with knowledge of its associated property name.
     */
    fun visit(name: String, jsonElement: JsonElement): Boolean = true

    /**
     * Used to indicate the that a visitor is ending its visit of a certain composite.
     */
    fun endVisit(jsonComposite: JsonComposite) {}
}

interface JsonElement {
    fun accept(visitor: Visitor) {}
    fun accept(visitor: Visitor, name: String): Boolean = true

    //para poder declarar aqui assim como por o addObserver também aqui foi necessário
    //colocar como objeto público, caso contrário temos que repetir o código nas classes filho.
    val observers: MutableList<JsonElementObserver>

    fun addObserver(observer: JsonElementObserver) {
        observers.add(observer)
    }
}

abstract class JsonComposite : JsonElement {
    abstract val elements: Any
    override val observers: MutableList<JsonElementObserver> = mutableListOf()
}

abstract class JsonLeaf<T>(val value: T) : JsonElement {

    override val observers: MutableList<JsonElementObserver> = mutableListOf()

    /**
     * Calls the visit method for the same JSON Leaf
     */
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
    /**
     * Calls the visit method for the same JSON Leaf with reference of its property name.
     */
    override fun accept(visitor: Visitor, name: String): Boolean {
        return visitor.visit(name, this)
    }
}

class JsonObject() : JsonComposite() {

    override val elements = mutableMapOf<String, JsonElement>()

    /**
     * Associates a JSON Element to this JSON Object.
     */
    fun addElement(name: String, value: JsonElement) {
        elements[name] = value
        observers.forEach {
            it.elementAdded(value)
        }

        //Este código é relativo a quando é adicionado algo a um filho,
        // não sabemos ainda como queremos reagir. O mesmo aplica-se ao jsonArray
        value.addObserver(object: JsonElementObserver {
            override fun elementAdded(element: JsonElement) {
                //queremos notificar de forma a indicar que algo foi adicionado a algum nível inferior:
                observers.forEach {
                    it.elementAdded(value)
                }

                //ou é melhor notificar de forma mais genérica:
                /*
                observers.forEach {
                    it.elementUpdate()
                }
                 */

                //por enquanto sinto que podemos continuar com o element added, assim como na
                //aula prática e ver no que dá
            }
        })
    }


    /**
     * Visits self and all children using the [visitChildren] method.
     */
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        visitChildren(visitor)
    }

    /**
     * Visits self with knowledge of its property name,
     * and visits all children using the [visitChildren] method if the value returned from its own visit is *true*.
     */
    override fun accept(visitor: Visitor, name: String): Boolean {
        if(visitor.visit(name, this))
            return visitChildren(visitor)
        return false
    }

    /**
     * Visits all children with their associeated property names as long as the value returned is *true*.
     */
    private fun visitChildren(visitor: Visitor): Boolean {
        elements.forEach {
            if(!it.value.accept(visitor, it.key)){
                visitor.endVisit(this)
                return false
            }
        }
        visitor.endVisit(this)
        return true
    }

    override fun toString(): String {
        return elements.toString()
    }
}

class JsonArray() : JsonComposite() {
    override val elements = mutableListOf<JsonElement>()

    /**
     * Associates a JSON Element to this JSON Array.
     */
    fun addElement(value: JsonElement) {
        elements.add(value)

        observers.forEach {
            it.elementAdded(value)
        }

        //Este código é relativo a quando é adicionado algo a um filho,
        // não sabemos ainda como queremos reagir.
        value.addObserver(object: JsonElementObserver {
            override fun elementAdded(element: JsonElement) {
                observers.forEach {
                    it.elementAdded(value)
                }
            }
        })
    }

    /**
     * Visits self and all children using the [visitChildren] method.
     */
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        visitChildren(visitor)
    }

    /**
     * Visits self with knowledge of its property name,
     * and visits all children using the [visitChildren] method if the value returned from its own visit is *true*.
     */
    override fun accept(visitor: Visitor, name: String): Boolean {
        if(visitor.visit(name, this)){
            visitChildren(visitor)
            return true
        }
        return false
    }

    /**
     * Visits all children.
     */
    private fun visitChildren(visitor: Visitor) {
        elements.forEach {
            it.accept(visitor)
        }
        visitor.endVisit(this)
    }

    override fun toString(): String {
        return elements.toString()
    }
}

/**
 * Class representing JSON String values as JSON Elements.
 */
class JsonString(value: String) : JsonLeaf<String>(value) {
    override fun toString(): String {
        return "\"$value\""
    }
}

/**
 * Class representing JSON Number values as JSON Elements.
 */
class JsonNumber(value: Number) : JsonLeaf<Number>(value) {
    override fun toString(): String {
        return value.toString()
    }
}

/**
 * Class representing JSON Boolean values as JSON Elements.
 */
class JsonBoolean(value: Boolean) : JsonLeaf<Boolean>(value) {
    override fun toString(): String {
        return value.toString()
    }
}

/**
 * Class representing JSON Null values as JSON Elements.
 */
class JsonNull : JsonLeaf<Any?>(null) {
    override fun toString(): String {
        return "null"
    }
}

interface JsonElementObserver {
    //fun elementModified(old: JsonElement, new: JsonElement)
    //fun elementDeleted(element: JsonElement)
    fun elementAdded(element: JsonElement)
}