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
        var standardList = mutableListOf<KClass<*>>()

        override fun visit(key: String, value: JSONElement) {
            if(key == property && value is JSONArray) {
                when(val firstElement = value.elements[0]) {
                    is JSONObject -> firstElement.elements.forEach { standardMap[it.key] = it.value::class }
                    is JSONLeaf<*> -> standardList.add(firstElement::class)
                }
                value.elements.forEach { it ->
                    when(it) {
                        is JSONObject -> {
                            if(standardMap.keys == it.elements.keys) {
                                it.elements.forEach {
                                    if(standardMap[it.key] != it.value::class) this.value = false
                                }
                            } else {
                                this.value = false
                            }
                        }
                        is JSONLeaf<*> -> {
                            if(it::class != standardList[0]) this.value = false
                        }
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
                if(!value.elements.all {value.elements[0].hasSameStructure(it) }) this.value = false
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

    val jobject5 = JSONObject()
    //jobject4.addElement("objeto", jobject5)
    jobject5.addElement("numero", JSONNumber(26503))
    jobject5.addElement("nome", JSONNumber(1))
    jobject5.addElement("internacional", JSONBoolean(false))

    val jobject6 = JSONObject()
    //jobject4.addElement("objeto", jobject5)
    jobject6.addElement("numero", JSONNumber(2653))
    jobject6.addElement("nome", JSONNumber(2))
    jobject6.addElement("internacional", JSONBoolean(false))

    val jobject7 = JSONObject()
    //jobject4.addElement("objeto", jobject5)
    jobject7.addElement("numero", JSONNumber(2653))
    jobject7.addElement("nome", JSONString("Afonso"))
    jobject7.addElement("internacional", JSONBoolean(false))

    val jarray2 = JSONArray()
    jarray2.addElement(JSONString("E1"))
    jarray2.addElement(JSONNumber(1))

    jobject2.addElement("objeto", jobject5)
    jobject3.addElement("objeto", jobject6)
    jobject4.addElement("objeto", jobject5)

    //jobject.addElement("extra", jobject2)

    println(jobject.getStructure())
    println(jobject.verifyArrayEquality("inscritos"))

    /**var auxArray = JSONArray()
    jobject.addElement("auxiliar", auxArray)

    var student4 = JSONObject()
    auxArray.addElement(student4)
    student4.addElement("numero", JSONString("teste"))
    student4.addElement("nome", JSONString("André Santos"))
    student4.addElement("internacional", JSONBoolean(false))

    var student5 = JSONObject()
    auxArray.addElement(student5)
    student5.addElement("numero", JSONString("teste"))
    student5.addElement("nome", JSONString("André Santo"))
    student5.addElement("internacional", JSONBoolean(false))

    println(jobject.verifyArrayEquality("auxiliar"))
    **/

}

