import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.*

interface Visitor {
    fun visit(jsonLeaf: JSONLeaf<*>) {}
    fun visit(jsonComposite: JSONComposite) {}
    fun visit(key: String, value: JSONElement) {}
    fun endVisit(jsonComposite: JSONComposite) {}
}

interface JSONElement {
    fun accept(visitor: Visitor) {}
    fun accept(visitor: Visitor, key: String) {}
}

abstract class JSONComposite : JSONElement {
    abstract val elements: Any
}

abstract class JSONLeaf<T>(val value: T) : JSONElement {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
    override fun accept(visitor: Visitor, key: String) {
        visitor.visit(key, this)
    }
}

class JSONObject() : JSONComposite() {
    override val elements = mutableMapOf<String, JSONElement>()
    fun addElement(name: String, value: JSONElement) {
        elements[name] = value
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        visitChildren(visitor)
    }

    override fun accept(visitor: Visitor, key: String) {
        visitor.visit(key, this)
        visitChildren(visitor)
    }

    private fun visitChildren(visitor: Visitor) {
        elements.forEach {
            it.value.accept(visitor, it.key)
        }
        visitor.endVisit(this)
    }

    override fun toString(): String {
        return elements.toString()
    }
}

class JSONArray() : JSONComposite() {
    override val elements = mutableListOf<JSONElement>()
    fun addElement(value: JSONElement) {
        elements.add(value)
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        visitChildren(visitor)
    }

    override fun accept(visitor: Visitor, key: String) {
        visitor.visit(key, this)
        visitChildren(visitor)
    }

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

class JSONString(value: String) : JSONLeaf<String>(value) {
    override fun toString(): String {
        return "\"$value\""
    }
}

class JSONNumber(value: Number) : JSONLeaf<Number>(value) {
    override fun toString(): String {
        return value.toString()
    }
}

class JSONBoolean(value: Boolean) : JSONLeaf<Boolean>(value) {
    override fun toString(): String {
        return value.toString()
    }
}

class JSONNull : JSONLeaf<Any?>(null) {
    override fun toString(): String {
        return "null"
    }
}

fun JSONObject.getValuesByProperty(property: String): List<JSONElement> {
    val result = object : Visitor {
        var elementList = mutableListOf<JSONElement>()
        override fun visit(key: String, value: JSONElement) {
            if(key == property) elementList.add(value)
        }
    }
    this.accept(result)
    return result.elementList
}

fun JSONObject.getJSONObjectWithProperty(list: List<String>): List<JSONObject> {
    val result = object : Visitor {
        var elementList = mutableListOf<JSONObject>()
        private var counter = 0
        private var uniqueList = list.distinct()
        override fun visit(jsonComposite: JSONComposite) {
            counter = 0
            if (jsonComposite is JSONObject) {
                jsonComposite.elements.keys.forEach {
                    if (list.contains(it)) counter++
                }
                if(counter == uniqueList.size && list.isNotEmpty()) elementList.add(jsonComposite)
            }
        }
    }

    this.accept(result)
    return result.elementList
}

fun JSONObject.verifyStructure(property: String, type: KClass<*>) : Boolean {
    val result = object : Visitor {
        var value = true
        override fun visit(key: String, value: JSONElement) {
            if(key == property && value::class != type) this.value = false
        }
    }
    this.accept(result)
    return result.value
}

fun JSONObject.verifyArrayEquality(property: String) : Boolean {
    val result = object : Visitor {
        var value = true
        var standardMap = mutableMapOf<String, KClass<*>>()

        override fun visit(key: String, value: JSONElement) {
            if(key == property && value is JSONArray) {

                if(!value.elements.all { value.elements[0]::class == it::class }) this.value = false

                val firstObject = value.elements[0]
                if(firstObject is JSONObject) { firstObject.elements.forEach { standardMap[it.key] = it.value::class } }

                value.elements.forEach { it ->
                    if(it is JSONObject) {
                        if (standardMap.keys == it.elements.keys) {
                            it.elements.forEach { if (standardMap[it.key] != it.value::class) this.value = false }
                        } else { this.value = false }
                    }
                }
            }
        }
    }
    this.accept(result)
    return result.value
}

fun JSONElement.hasSameStructure(that: JSONElement): Boolean {
    if(this::class != that::class )
        return false

    when(this) {
        is JSONLeaf<*> -> return true
        is JSONArray -> {
            if(this.elements.size != (that as JSONArray).elements.size)
                return false
            this.elements.zip(that.elements).forEach {
                if(!it.first.hasSameStructure(it.second))
                    return false
            }
        }
        is JSONObject -> {
            if(this.elements.size != (that as JSONObject).elements.size)
                return false
            if(this.elements.keys != that.elements.keys)
                return false
            for(key in this.elements.keys){
                if(!this.elements[key]!!.hasSameStructure(that.elements[key]!!))
                    return false
            }
        }
    }

    return true
}
fun JSONObject.verifyArrayEqualityAlt(property: String): Boolean {
    val result = object : Visitor {
        var value = true
        override fun visit(key: String, value: JSONElement) {
            if(key == property && value is JSONArray) {
                if(!value.elements.all {value.elements[0].hasSameStructure(it)}) this.value = false
            }
        }

    }
    this.accept(result)
    return result.value
}

fun JSONElement.getStructure() : String {
    val structure = object : Visitor {
        var structure: String = ""
        private var prefix: String = ""
        private var prefix2: String= ""

        override fun visit(key: String, value: JSONElement) {
            when(value) {
                is JSONLeaf<*> -> {
                    structure += prefix2 + "\n" + prefix + (if (key.isEmpty()) "" else "\"$key\" : ") + value
                    prefix2 = ","
                }

                is JSONArray -> updateStructure(key,value)

                is JSONObject -> updateStructure(key,value)
            }
        }

        private fun updateStructure(key: String, jsonComposite: JSONComposite) {
            if (structure.isNotEmpty()) structure += prefix2 + "\n"
            structure += "$prefix\"$key\" : " + if(jsonComposite is JSONObject) "{" else "["
            updatePrefix()
        }

        override fun visit(value: JSONLeaf<*>) {
            structure += prefix2 + "\n" + prefix + value
            prefix2 = ","
        }

        override fun visit(jsonComposite: JSONComposite) {
            if(structure.isNotEmpty()) structure += prefix2 + "\n"
            structure += prefix + if(jsonComposite is JSONObject) "{" else "["
            updatePrefix()
        }

        private fun updatePrefix() {
            prefix2 = ""
            prefix += "\t"
        }

        override fun endVisit(jsonComposite: JSONComposite) {
            prefix = prefix.dropLast(1)
            structure += "\n" + prefix + (if (jsonComposite is JSONObject) "}" else "]")
        }
    }
    this.accept(structure)
    return structure.structure
}

/*
 * Instantiates this object as a JSON Object through reflection
 * considering any JSON annotations associated to any properties.
 *
 * @return the JSON Object of the instantiated class.
 */
fun Any.toJson(): JSONObject {
    val rootObject = JSONObject()
    val list = this::class.dataClassFields
    for (it in list) {
        if (it.hasAnnotation<JsonExclude>())
            continue

        val name = if (it.hasAnnotation<JsonName>()) it.findAnnotation<JsonName>()!!.name else it.name

        if (it.hasAnnotation<JsonAsString>())
            rootObject.addElement(name, JSONString(it.call(this).toString()))
        else {
            val element: JSONElement = it.call(this).mapAsJson()
            rootObject.addElement(name, element)
        }
    }
    return rootObject
}

/*
 * Maps this object to its corresponding JSON Element object.
 *
 * @return the JSON element of the corresponding object.
 */
private fun Any?.mapAsJson(): JSONElement =
    when (this) {
        is Number -> JSONNumber(this)
        is String -> JSONString(this)
        is Char -> JSONString(this.toString())
        is Boolean -> JSONBoolean(this)
        is Enum<*> -> JSONString(this.name)
        is Map<*, *> -> this.getMapElements()
        is Iterable<*> -> this.getArrayElements()
        null -> JSONNull()
        else -> if (this::class.isData) this.toJson() else JSONNull()
    }

/*
 * Obtains the corresponding JSON Element of each item in this iterable.
 *
 * @return JSON Array containing all elements of the iterable as JSON Elements.
 */
private fun Iterable<*>.getArrayElements(): JSONArray {
    val jsonArray = JSONArray()

    this.forEach { arrayElement ->
        jsonArray.addElement(arrayElement.mapAsJson())
    }
    return jsonArray
}

/*
 * Obtains the corresponding JSON Element and name of each item in this map.
 *
 * @return JSON Object containing all elements of the map as JSON Elements with their corresponding names.
 */
private fun Map<*, *>.getMapElements(): JSONObject {
    val jsonObject = JSONObject()

    this.forEach { mapEntry ->
        jsonObject.addElement(mapEntry.key.toString(), mapEntry.value.mapAsJson())
    }
    return jsonObject
}

/*
 * Obtains a list containing all KProperties of this KClass, this KClass must be a data class.
 *
 * @return list of all KProperties contained in this KClass if it is a data class.
 * @throws IllegalArgumentException if this KClass does not correspond to a data class
 */
private val KClass<*>.dataClassFields: List<KProperty<*>>
    get() {
        require(isData) { "instance must be data class" }
        return primaryConstructor!!.parameters.map { p ->
            declaredMemberProperties.find { it.name == p.name }!!
        }
    }


/*
 * Annotations used to tag class properties used when instantiating the corresponding JSON Object.
 */

/*
 * Excludes a property from being instantiated.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude()

/*
 * Sets a custom name for the json element of a certain property,
 * when instantiating the corresponding JSON Object.
 *
 * @param name the custom name to be used for the json element.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class JsonName(val name: String)

/*
 * Forces a certain property to be considered a string when instantiating the corresponding JSON Object.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class JsonAsString()

fun main() {
    val jobject = JSONObject()
    jobject.addElement("uc", JSONString("PA"))
    jobject.addElement("ects", JSONNumber(6.0))
    jobject.addElement("data-exame", JSONNull())
    val jarray = JSONArray()
    jobject.addElement("inscritos", jarray)

    val jobject2 = JSONObject()
    jarray.addElement(jobject2)
    jobject2.addElement("numero", JSONNumber(101101))
    jobject2.addElement("nome", JSONString("Dave Farley"))
    jobject2.addElement("internacional", JSONBoolean(true))

    val jobject3 = JSONObject()
    jarray.addElement(jobject3)
    jobject3.addElement("numero", JSONNumber(101102))
    jobject3.addElement("nome", JSONString("Martin Fowler"))
    jobject3.addElement("internacional", JSONBoolean(true))

    val jobject4 = JSONObject()
    jarray.addElement(jobject4)
    jobject4.addElement("numero", JSONNumber(26503))
    jobject4.addElement("nome", JSONString("André Santo"))
    jobject4.addElement("internacional", JSONBoolean(false))

    println(jobject.getStructure())

    val jarray2 = JSONArray()
    jarray2.addElement(JSONString("E1"))
    jarray2.addElement(JSONNumber(1))
}

