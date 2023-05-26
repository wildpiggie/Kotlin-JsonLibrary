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
}

abstract class JsonComposite : JsonElement

abstract class JsonLeaf<T>(val value: T) : JsonElement {
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

    val elements = mutableMapOf<String, JsonElement>()

    //para poder declarar aqui assim como por o addObserver também aqui foi necessário
    //colocar como objeto público, caso contrário temos que repetir o código nas classes filho.
    private val observers: MutableList<JsonObjectObserver> = mutableListOf()

    fun addObserver(observer: JsonObjectObserver) {
        observers.add(observer)
    }

    /**
     * Associates a JSON Element to this JSON Object.
     */
    fun addElement(name: String, value: JsonElement) {
        elements[name] = value
        observers.forEach {
            it.elementAdded(name, value)
        }
    }

    /**
     * Associates a JSON Element to this JSON Object at a specific index.
     */
    fun addElement(name: String, value: JsonElement, index: Int) {
        elements[name] = value
        observers.forEach {
            it.elementAdded(name, value, index)
        }
    }

    /**
     * Removes a JSON Element from this JSON Object.
     */
    fun removeElement(name: String, index: Int) {
        elements.remove(name)
        observers.forEach {
            it.elementRemoved(name, index)
        }
    }

    /**
     * Modifies the value of JSON Element from this JSON Object.
     */
    fun modifyElement(name: String, newValue: JsonElement, index: Int) {
        elements[name] = newValue
        observers.forEach {
            it.elementModified(name, newValue, index)
        }
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
    val elements = mutableListOf<JsonElement>()

    private val observers: MutableList<JsonArrayObserver> = mutableListOf()

    fun addObserver(observer: JsonArrayObserver) {
        observers.add(observer)
    }

    /**
     * Associates a JSON Element to this JSON Array.
     */
    fun addElement(value: JsonElement) {
        elements.add(value)

        observers.forEach {
            it.elementAdded(value)
        }
    }

    /**
     * Associates a JSON Element to this JSON Array at the specified index.
     */
    fun addElement(value: JsonElement, index: Int) {
        elements.add(index, value)

        observers.forEach {
            it.elementAdded(value, index)
        }
    }

    /**
     * Removes a JSON Element from this JSON Array.
     */
    fun removeElement(index: Int) {
        elements.removeAt(index)

        observers.forEach {
            it.elementRemoved(index)
        }
    }

    /**
     * Modifies the value of JSON Element from this JSON Array.
     */
    fun modifyElement(index: Int, newValue: JsonElement) {
        elements[index] = newValue
        observers.forEach {
            it.elementModified(index, newValue)
        }
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

interface JsonObjectObserver {
    fun elementRemoved(name: String) {}
    fun elementRemoved(name: String, index: Int)
    fun elementAdded(name: String, value: JsonElement) {}
    fun elementAdded(name: String, value: JsonElement, index: Int) {}
    fun elementModified(name: String, newValue: JsonElement, index: Int) {}
}

interface JsonArrayObserver {
    fun elementRemoved(index: Int)
    fun elementAdded(value: JsonElement)
    fun elementAdded(value: JsonElement, index: Int)
    fun elementModified(index: Int, newValue: JsonElement)
}