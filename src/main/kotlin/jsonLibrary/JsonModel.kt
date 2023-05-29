package jsonLibrary

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

/**
 * This interface represents any type of JSON Element.
 */
interface JsonElement {
    fun accept(visitor: Visitor) {}
    fun accept(visitor: Visitor, name: String): Boolean = true
}

/**
 * This abstract class represents any type of composite JSON Element.
 * Namely: JSON Objects and JSON Arrays.
 */
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

/**
 * Class representing JSON Objects as JSON Elements.
 */
class JsonObject : JsonComposite() {

    val elements = mutableMapOf<String, JsonElement>()

    private val observers: MutableList<JsonObjectObserver> = mutableListOf()

    fun addObserver(observer: JsonObjectObserver) {
        observers.add(observer)
    }

    /**
     * Associates a JSON Element to this JSON Object.
     */
    fun addElement(name: String, value: JsonElement) {
        elements[name] = value
        val index = elements.keys.indexOf(name)
        observers.forEach {
            it.elementAdded(name, value, index)
        }
    }

    /**
     * Removes a JSON Element from this JSON Object.
     */
    fun removeElement(name: String) {
        val index = elements.keys.indexOf(name)
        elements.remove(name)
        observers.forEach {
            it.elementRemoved(name, index)
        }
    }

    /**
     * Modifies the value of JSON Element from this JSON Object.
     */
    fun modifyElement(name: String, newValue: JsonElement) {
        elements[name] = newValue
        val index = elements.keys.indexOf(name)
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

/**
 * Class representing JSON Arrays as JSON Elements.
 */
class JsonArray : JsonComposite() {
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

/**
 * This interface allows for the creation of an observer to detect if a certain JSON Object has been altered.
 * It can track if an object has been added, removed, or modified.
 */
interface JsonObjectObserver {
    fun elementAdded(name: String, value: JsonElement, index: Int)
    fun elementRemoved(name: String, index: Int)
    fun elementModified(name: String, newValue: JsonElement, index: Int)
}

/**
 * This interface allows for the creation of an observer to detect if a certain JSON Array has been altered.
 * It can track if an object has been added (to a specific index or to the end of the array), removed, or modified.
 */
interface JsonArrayObserver {
    fun elementAdded(value: JsonElement)
    fun elementAdded(value: JsonElement, index: Int)
    fun elementRemoved(index: Int)
    fun elementModified(index: Int, newValue: JsonElement)
}