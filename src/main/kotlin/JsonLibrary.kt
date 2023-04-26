import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.*

interface Visitor {
    fun visit(jsonLeaf: JsonLeaf<*>) {}
    fun visit(jsonComposite: JsonComposite) {}
    fun visit(key: String, jsonElement: JsonElement) {}
    fun endVisit(jsonComposite: JsonComposite) {}
}

interface JsonElement {
    fun accept(visitor: Visitor) {}
    fun accept(visitor: Visitor, name: String) {}
}

abstract class JsonComposite : JsonElement {
    abstract val elements: Any
}

abstract class JsonLeaf<T>(val value: T) : JsonElement {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
    override fun accept(visitor: Visitor, name: String) {
        visitor.visit(name, this)
    }
}

class JsonObject() : JsonComposite() {
    override val elements = mutableMapOf<String, JsonElement>()
    fun addElement(name: String, value: JsonElement) {
        elements[name] = value
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        visitChildren(visitor)
    }

    override fun accept(visitor: Visitor, name: String) {
        visitor.visit(name, this)
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

class JsonArray() : JsonComposite() {
    override val elements = mutableListOf<JsonElement>()
    fun addElement(value: JsonElement) {
        elements.add(value)
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        visitChildren(visitor)
    }

    override fun accept(visitor: Visitor, name: String) {
        visitor.visit(name, this)
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

class JsonString(value: String) : JsonLeaf<String>(value) {
    override fun toString(): String {
        return "\"$value\""
    }
}

class JsonNumber(value: Number) : JsonLeaf<Number>(value) {
    override fun toString(): String {
        return value.toString()
    }
}

class JsonBoolean(value: Boolean) : JsonLeaf<Boolean>(value) {
    override fun toString(): String {
        return value.toString()
    }
}

class JsonNull : JsonLeaf<Any?>(null) {
    override fun toString(): String {
        return "null"
    }
}

fun JsonObject.getValuesOfProperty(name: String): List<JsonElement> {
    val result = object : Visitor {
        val elementList = mutableListOf<JsonElement>()
        override fun visit(key: String, jsonElement: JsonElement) {
            if(key == name) elementList.add(jsonElement)
        }
    }
    this.accept(result)
    return result.elementList
}

fun JsonObject.getJsonObjectWithProperties(properties: List<String>): List<JsonObject> {
    val result = object : Visitor {
        val elementList = mutableListOf<JsonObject>()
        override fun visit(jsonComposite: JsonComposite) {
            if (jsonComposite is JsonObject)
                if(jsonComposite.elements.keys.containsAll(properties))
                    elementList.add(jsonComposite)
        }
    }
    this.accept(result)
    return result.elementList
}

//Não termina logo que chega a um de tipo errado, há forma de mudar o comportamento?
fun JsonObject.isPropertyOfType(name: String, type: KClass<*>) : Boolean {
    val result = object : Visitor {
        var value = true
        override fun visit(key: String, jsonElement: JsonElement) {
            if(key == name && jsonElement::class != type)
                this.value = false
        }
    }
    this.accept(result)
    return result.value
}

fun JsonObject.isArrayStructureHomogenousShallow(arrayName: String) : Boolean {
    val result = object : Visitor {
        var value = true
        var standardMap = mutableMapOf<String, KClass<*>>()

        override fun visit(key: String, jsonElement: JsonElement) {
            if(key == arrayName && jsonElement is JsonArray) {
                if(!jsonElement.elements.all { jsonElement.elements.first()::class == it::class }) this.value = false

                val firstElement = jsonElement.elements.first()

                if(this.value && firstElement is JsonObject) {
                    firstElement.elements.forEach { standardMap[it.key] = it.value::class }

                    jsonElement.elements.forEach { arrayElement ->
                        if (standardMap.keys == (arrayElement as JsonObject).elements.keys) {
                            arrayElement.elements.forEach { if (standardMap[it.key] != it.value::class) this.value = false }
                        } else { this.value = false }
                    }
                }
            }
        }
    }
    this.accept(result)
    return result.value
}

// Possivelmente util ser public
private fun JsonElement.hasSameStructure(that: JsonElement): Boolean {
    if(this::class != that::class )
        return false

    when(this) {
        is JsonArray -> {
            if(this.elements.size != (that as JsonArray).elements.size)
                return false
            this.elements.zip(that.elements).forEach {
                if(!it.first.hasSameStructure(it.second))
                    return false
            }
        }
        is JsonObject -> {
            if(this.elements.size != (that as JsonObject).elements.size)
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
fun JsonObject.isArrayStructureHomogenousDeep(name: String): Boolean {
    val result = object : Visitor {
        var value = true
        override fun visit(key: String, jsonElement: JsonElement) {
            if(key == name && jsonElement is JsonArray)
                if(!jsonElement.elements.all {jsonElement.elements[0].hasSameStructure(it)}) this.value = false
        }
    }
    this.accept(result)
    return result.value
}

fun JsonElement.getStructure() : String {
    val structure = object : Visitor {
        var structure: String = ""
        private var prefix: String = ""
        private var prefix2: String= ""

        override fun visit(key: String, jsonElement: JsonElement) {
            when(jsonElement) {
                is JsonLeaf<*> -> {
                    structure += prefix2 + "\n" + prefix + (if (key.isEmpty()) "" else "\"$key\" : ") + jsonElement
                    prefix2 = ","
                }

                is JsonArray -> updateStructure(key,jsonElement)

                is JsonObject -> updateStructure(key,jsonElement)
            }
        }

        fun updateStructure(name: String, jsonComposite: JsonComposite) {
            if (structure.isNotEmpty()) structure += prefix2 + "\n"
            structure += "$prefix\"$name\" : " + if(jsonComposite is JsonObject) "{" else "["
            updatePrefix()
        }

        override fun visit(jsonLeaf: JsonLeaf<*>) {
            structure += prefix2 + "\n" + prefix + jsonLeaf
            prefix2 = ","
        }

        override fun visit(jsonComposite: JsonComposite) {
            if(structure.isNotEmpty()) structure += prefix2 + "\n"
            structure += prefix + if(jsonComposite is JsonObject) "{" else "["
            updatePrefix()
        }

        fun updatePrefix() {
            prefix2 = ""
            prefix += "\t"
        }

        override fun endVisit(jsonComposite: JsonComposite) {
            prefix = prefix.dropLast(1)
            structure += "\n" + prefix + (if (jsonComposite is JsonObject) "}" else "]")
        }
    }
    this.accept(structure)
    return structure.structure
}

/**
 * Maps this object to its corresponding JSON Element object.
 *
 * @return the JSON element of the corresponding object.
 */
fun Any?.toJson(): JsonElement =
    when (this) {
        is Number -> JsonNumber(this)
        is String -> JsonString(this)
        is Char -> JsonString(this.toString())
        is Boolean -> JsonBoolean(this)
        is Enum<*> -> JsonString(this.name)
        is Map<*, *> -> this.getMapElements()
        is Iterable<*> -> this.getArrayElements()
        null -> JsonNull()
        else -> if (this::class.isData) this.getObjectElements() else JsonNull()
    }

/**
 * Instantiates this object as a JSON Object through reflection
 * considering any JSON annotations associated to any properties.
 *
 * @return JSON Object of the instantiated class.
 */
private fun Any.getObjectElements(): JsonObject {

    val rootObject = JsonObject()
    val list = this::class.dataClassFields
    for (it in list) {
        if (it.hasAnnotation<JsonExclude>())
            continue

        val name = if (it.hasAnnotation<JsonName>()) it.findAnnotation<JsonName>()!!.name else it.name

        if (it.hasAnnotation<JsonAsString>())
            rootObject.addElement(name, JsonString(it.call(this).toString()))
        else {
            val element: JsonElement = it.call(this).toJson()
            rootObject.addElement(name, element)
        }
    }
    return rootObject
}

/**
 * Obtains the corresponding JSON Element of each item in this iterable.
 *
 * @return JSON Array containing all elements of the iterable as JSON Elements.
 */
private fun Iterable<*>.getArrayElements(): JsonArray {
    val jsonArray = JsonArray()

    this.forEach { arrayElement ->
        jsonArray.addElement(arrayElement.toJson())
    }
    return jsonArray
}

/**
 * Obtains the corresponding JSON Element and name of each item in this map.
 *
 * @return JSON Object containing all elements of the map as JSON Elements with their corresponding names.
 */
private fun Map<*, *>.getMapElements(): JsonObject {
    val jsonObject = JsonObject()

    this.forEach { mapEntry ->
        jsonObject.addElement(mapEntry.key.toString(), mapEntry.value.toJson())
    }
    return jsonObject
}

/**
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

/**
 * Excludes a property from being instantiated.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class JsonExclude()

/**
 * Sets a custom name for the json element of a certain property,
 * when instantiating the corresponding JSON Object.
 *
 * @param name the custom name to be used for the json element.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class JsonName(val name: String)

/**
 * Forces a certain property to be considered a string when instantiating the corresponding JSON Object.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class JsonAsString()

fun main() {
    val jobject = JsonObject()
    jobject.addElement("uc", JsonString("PA"))
    jobject.addElement("ects", JsonNumber(6.0))
    jobject.addElement("data-exame", JsonNull())
    val jarray = JsonArray()
    jobject.addElement("inscritos", jarray)

    val jobject2 = JsonObject()
    jarray.addElement(jobject2)
    jobject2.addElement("numero", JsonNumber(101101))
    jobject2.addElement("nome", JsonString("Dave Farley"))
    jobject2.addElement("internacional", JsonBoolean(true))

    val jobject3 = JsonObject()
    jarray.addElement(jobject3)
    jobject3.addElement("numero", JsonNumber(101102))
    jobject3.addElement("nome", JsonString("Martin Fowler"))
    jobject3.addElement("internacional", JsonBoolean(true))

    val jobject4 = JsonObject()
    jarray.addElement(jobject4)
    jobject4.addElement("numero", JsonNumber(26503))
    jobject4.addElement("nome", JsonString("André Santo"))
    jobject4.addElement("internacional", JsonBoolean(false))

    println(jobject.getStructure())

    val jarray2 = JsonArray()
    jarray2.addElement(JsonString("E1"))
    jarray2.addElement(JsonNumber(1))
}
