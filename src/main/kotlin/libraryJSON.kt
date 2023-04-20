//import java.util.StringJoiner
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

interface Visitor {
    fun visit(jsonLeaf: JSONLeaf<*>) {}
    fun visit(jsonComposite: JSONComposite) {}
    fun visit(jsonObject: JSONObject) {}
    fun visit(jsonArray: JSONArray) {}
    fun visit(key: String, value: JSONElement) {}
    fun visit(value: JSONElement) {}
    fun endVisit(jsonComposite: JSONComposite) {}

}

interface JSONElement {
    fun accept(visitor: Visitor) {}
    fun accept(visitor: Visitor, key: String) {}
}

abstract class JSONComposite : JSONElement {
    abstract val elements: Any
}

abstract class JSONLeaf<T> (val value: T) : JSONElement {
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
        return "\"" + value + "\""
    }
}
class JSONNumber(value: Number) : JSONLeaf<Number>(value) {
    override fun toString(): String {
        return value.toString()
    }
}
class JSONBoolean(value: Boolean) :JSONLeaf<Boolean>(value) {
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

        override fun visit(jsonComposite: JSONComposite) {
            if(jsonComposite is JSONObject) jsonComposite.elements.forEach { if(it.key == property) elementList.add(it.value) }
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
                    if(list.contains(it)) counter++
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

        override fun visit(jsonComposite: JSONComposite) {
            if(jsonComposite is JSONObject) {
                jsonComposite.elements.forEach {
                    if(it.key == property) {
                        if(it.value::class != type) value = false
                    }
                }
            }
        }
    }

    this.accept(result)
    return result.value
}

fun JSONObject.verifyArrayEquality(property: String) : Boolean {
    val result = object : Visitor {
        var value = false
        override fun visit(jsonComposite: JSONComposite) {
            if(jsonComposite is JSONObject) {
                jsonComposite.elements.forEach { it ->
                    if(it.key == property && it.value is JSONArray) {
                        (it.value as JSONArray).elements.forEach {
                            println(it::class)
                        }
                    }
                }
            }
        }
    }
    this.accept(result)
    return result.value
}

fun JSONElement.getStructure() : String {
    val strucutre = object : Visitor {
        var structure: String = ""
        private var prefix: String = ""
        private var prefix2: String= ""

        override fun visit(key: String, value: JSONElement) {
            when(value) {
                is JSONLeaf<*> -> {
                    structure += prefix2 + "\n" + prefix + (if (key.isEmpty()) "" else "\"$key\" : ") + value
                    prefix2 = ","
                }

                is JSONArray -> {
                    if(structure.isNotEmpty()) structure += prefix2 + "\n"
                    structure += "$prefix\"$key\" : ["
                    prefix2 = ""
                    prefix += "\t"
                }

                is JSONObject -> {
                    if(structure.isNotEmpty()) structure += prefix2 + "\n"
                    structure += "$prefix\"$key\" : {"
                    prefix2 = ""
                    prefix += "\t"
                }
            }
        }

        override fun visit(value: JSONLeaf<*>) {
            structure += prefix2 + "\n" + prefix + value
            prefix2 = ","

        }

        override fun visit(jsonObject: JSONObject) {
            if(structure.isNotEmpty()) structure += prefix2 + "\n"
            structure += "$prefix{"
            prefix2 = ""
            prefix += "\t"
        }

        override fun visit(jsonArray: JSONArray) {
            if(structure.isNotEmpty()) structure += prefix2 + "\n"
            structure += "$prefix["
            prefix2 = ""
            prefix += "\t"
        }

        override fun endVisit(jsonComposite: JSONComposite) {
            prefix = prefix.dropLast(1)
            structure += "\n" + prefix + (if (jsonComposite is JSONObject) "}" else "]")
        }

    }
    this.accept(strucutre)
    return strucutre.structure
}


val KClass<*>.dataClassFields: List<KProperty<*>>
    get() {
        require(isData) { "instance must be data class" }
        return primaryConstructor!!.parameters.map { p ->
            declaredMemberProperties.find { it.name == p.name }!!
        }
    }

// saber se um KClassifier é um enumerado
val KClassifier?.isEnum: Boolean
    get() = (this is KClass<*>) && this.isSubclassOf(Enum::class)

// obter uma lista de constantes de um tipo enumerado
val <T : Any> KClass<T>.enumConstants: List<T> get() {
    require(isEnum) { "instance must be enum" }
    return java.enumConstants.toList()
}

class JSONGenerator(){

}

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

    val jarray2 = JSONArray()
    jarray2.addElement(JSONString("E1"))
    jarray2.addElement(JSONNumber(1))

    jobject.addElement("extra", jobject2)

    println(jobject.getStructure())

}

