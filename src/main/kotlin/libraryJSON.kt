import java.util.StringJoiner
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

/**
 * confirmar se a forma como fizemos esta certo
 * com o generico no JSONLeaf da problemas no visitor -> so conseguimos definir um tipo
 * as fun de search a usar os visitor estao feitas corretamente?
 * nos testes na funçao de obter os valores associados as propriedades, temos uma lista de JSONElements, por isso
 * ao comparar uma lista de 2 objectos com o resultado, embora seja o mesmo nao vai dar certo por nao serem data classes
 * Testatasmos a comparar os values?
 * Na funçao para obter os objetos associados às propriedades, podemos fazer a pesquisa toda quando entramos no composto
 * **/

interface Visitor {
    fun visit(jsonLeaf: JSONLeaf<*>) {}
    fun visit(jsonComposite: JSONComposite) {}
    fun endVisit(jsonComposite: JSONComposite) {}

}
interface JSONElement {
    fun accept(visitor: Visitor) {}
}

abstract class JSONComposite : JSONElement {
    abstract val elements: Any
}

abstract class JSONLeaf<T> (val value: T) : JSONElement {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

class JSONObject() : JSONComposite() {
    override val elements = mutableMapOf<String, JSONElement>()
    fun addElement(name: String, value: JSONElement) {
        elements[name] = value
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
        elements.values.forEach {
            it.accept(visitor)
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
        return "\"" + value.toString() + "\""
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
        return "null" // ??
    }
}

fun JSONObject.getValuesByProperty(property: String): List<JSONElement> {
    val result = object : Visitor {
        var elementList = mutableListOf<JSONElement>()
        private var depth: Int = 0
        private val propertyMap = mutableMapOf<Int, MutableList<String>>()


        override fun visit(jsonLeaf: JSONLeaf<*>) {
            val name = propertyMap[depth]?.removeFirst()
            if (name == property) {
                elementList.add(jsonLeaf)
            }
        }

        override fun visit(jsonComposite: JSONComposite) {
            val name = propertyMap[depth]?.removeFirstOrNull()
            if (name == property) {
                elementList.add(jsonComposite)
            }
            if (jsonComposite is JSONObject) {
                propertyMap[++depth] = jsonComposite.elements.keys.toMutableList()
            }
        }


    }

    this.accept(result)
    return result.elementList
}

fun JSONObject.getJSONObjectWithPropertyAlt(list: List<String>): MutableList<JSONObject> {
    val result = object : Visitor {
        var elementList = mutableListOf<JSONObject>()
        private var counter = 0

        override fun visit(jsonComposite: JSONComposite) {
            counter = 0
            if (jsonComposite is JSONObject) {
                jsonComposite.elements.keys.forEach {
                    if(list.contains(it)) counter++
                }
                if(counter == list.size && list.isNotEmpty()) elementList.add(jsonComposite)
            }
        }
    }

    this.accept(result)
    return result.elementList
}

fun JSONElement.getStructure() : String {
    val structure = object  : Visitor {
        var structure: String = ""
        private var prefix: String = ""
        private var prefix2: String= ""
        private var depth: Int = 0
        private val propertyMap = mutableMapOf<Int, MutableList<String>>()

        override fun visit(jsonLeaf: JSONLeaf<*>) {
            val name = propertyMap[depth]?.removeFirst()
            structure += prefix2 + "\n" + prefix + (if(name.isNullOrEmpty()) "" else "\"$name\" : ") + jsonLeaf
            prefix2 = ","
        }

        override fun visit(jsonComposite: JSONComposite) {
            if (structure.isNotEmpty()) {
                structure += prefix2 + "\n"
            }

            val name = propertyMap[depth]?.removeFirstOrNull()
            structure += prefix + (if (name == null) "" else "\"$name\" : ") + if (jsonComposite is JSONObject) "{" else "["
            prefix2 = ""
            prefix += "\t"

            if (jsonComposite is JSONObject) {
                propertyMap[++depth] = jsonComposite.elements.keys.toMutableList()
            }
        }

        override fun endVisit(jsonComposite: JSONComposite) {
            depth--
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

    val jarray2 = JSONArray()
    jarray2.addElement(JSONString("E1"))
    jarray2.addElement(JSONNumber(1))

}

