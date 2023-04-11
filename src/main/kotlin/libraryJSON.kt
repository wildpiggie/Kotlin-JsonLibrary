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
    fun visit(jsonLeaf: JSONLeaf) {}
    fun visit(jsonComposite: JSONComposite) {}
    fun endVisit(jsonComposite: JSONComposite) {}

}
interface JSONElement {
    fun accept(visitor: Visitor) {}

    /**
    val toText: String
        get() {
            var text = ""

            if(this != null) {
                text = name
            } else if (this is JSONObject) {
                text = "{"
            }


            fun JSONElement.aux(prefix: String) {
                if(this is JSONComposite) {
                    children.forEach {
                        //text += prefix + " " + it.name + (if(it is JSONLeaf) " : " + it.value + "," else if(it is JSONObject) " {" else " [")
                        if (it.name == null) {
                            when (it) {
                                is JSONObject -> text += "$prefix  { "
                                is JSONArray -> text += "$prefix [ "
                            }
                        }
                        else {
                            when (it) {
                                is JSONLeaf -> text += prefix + it.name + " : " + it.value + ","
                                is JSONObject -> text += prefix + it.name + " : " + " { "
                                is JSONArray -> text += prefix + it.name + " : " + " [ "
                            }
                        }

                        if (it is JSONComposite) {
                            it.aux("$prefix \t")
                        }
                    }

                    when(this) {
                        is JSONObject -> text += "$prefix }"
                        is JSONArray -> text += "$prefix ]"
                    }

                }
            }

            this.aux("\n")
            return text
        }
    **/
}

abstract class JSONComposite : JSONElement {
    abstract val elements: Any
}

abstract class JSONLeaf (val value: Any?) : JSONElement {
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
class JSONString(value: String) : JSONLeaf(value) {
    override fun toString(): String {
        return "\"" + value.toString() + "\""
    }
}
class JSONNumber(value: Number) : JSONLeaf(value) {
    override fun toString(): String {
        return value.toString()
    }
}
class JSONBoolean(value: Boolean) :JSONLeaf(value) {
    override fun toString(): String {
        return value.toString()
    }
}
class JSONNull : JSONLeaf(null) {
    override fun toString(): String {
        return "null" // ??
    }
}

fun JSONObject.getValuesByProperty(property: String): List<JSONElement>{
    val result = object : Visitor {
        var elementList = mutableListOf<JSONElement>()
        private var depth: Int = 0
        private val propertyMap = mutableMapOf<Int, MutableList<String>>()


        override fun visit(jsonLeaf: JSONLeaf) {
            val name = propertyMap[depth]?.removeFirst()
            if(name == property) {
                elementList.add(jsonLeaf)
            }
        }

        override fun visit(jsonComposite: JSONComposite) {
            val name = propertyMap[depth]?.removeFirstOrNull()
            if(name == property) {
                elementList.add(jsonComposite) // fazemos um toString aqui ou?
            }
            if(jsonComposite is JSONObject) {
                propertyMap[++depth] = jsonComposite.elements.keys.toMutableList()
            }
        }

        
    }
    this.accept(result)
    return result.elementList
}
fun JSONObject.getJSONObjectWithProperty(list: List<String>): MutableList<JSONObject> {
    val result = object : Visitor {
        var elementList = mutableListOf<JSONObject>()
        private var depth: Int = 0
        private val propertyMap = mutableMapOf<Int, MutableList<String>>()
        private val parentMap = mutableMapOf<Int, JSONComposite>()
        private var counter = 0

        override fun visit(jsonLeaf: JSONLeaf) {
            val name = propertyMap[depth]?.removeFirst()
            if(list.contains(name)) counter++
            if (propertyMap[depth]?.size == 0 && counter == list.size) elementList.add(parentMap[depth] as JSONObject)
        }

        override fun visit(jsonComposite: JSONComposite) {
            val name = propertyMap[depth]?.removeFirstOrNull()
            //if(list.contains(name)) counter++
            //if (propertyMap[depth]?.size == 0 && counter == list.size) elementList.add(parentMap[depth] as JSONObject)
            //println(parentMap[depth])

            if(jsonComposite is JSONObject) {
                counter = 0
                propertyMap[++depth] = jsonComposite.elements.keys.toMutableList()
                parentMap[depth] = jsonComposite
            }
        }

        /** override fun endVisit(jsonComposite: JSONComposite) {
            if(jsonComposite is JSONObject && counter == list.size) elementList.add(jsonComposite as JSONObject)
        } **/


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

//TODO perceber como fazer a virgula dos ] }
fun JSONElement.getStructure() : String {
    val structure = object  : Visitor {
        var structure: String = ""
        private var prefix: String = ""
        private var prefix2: String= ""
        private var prefix3: String = ""
        private var depth: Int = 0
        private val propertyMap = mutableMapOf<Int, MutableList<String>>()

        override fun visit(jsonLeaf: JSONLeaf) {
            val name = propertyMap[depth]?.removeFirst()
            structure += prefix2 + "\n" + prefix + (if(name.isNullOrEmpty()) "" else "\"$name\" : ") + jsonLeaf
            prefix3 = ","
            prefix2 = ","
        }

        override fun visit(jsonComposite: JSONComposite) {
            if (structure.isNotEmpty()) {
                structure += "\n"
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
            //prefix3 = if(depth == 0) "," else ""
            depth--
            prefix = prefix.dropLast(1)
            structure += "\n" + prefix + (if (jsonComposite is JSONObject) "}" else "]") + prefix3
            prefix3 = ""

        }
    }
    this.accept(structure)
    return structure.structure
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

    val jarray2 = JSONArray()
    jarray2.addElement(JSONString("E1"))
    jarray2.addElement(JSONNumber(1))

    /**
    val t1 = jobject.getValuesByProperty("numero")
    t1.forEach { println(it) }
    val t2 = jobject.getJSONObjectWithProperty(listOf("numero", "nome"))
    t2.forEach { println(it.toString()) }
    **/

    //println(jobject.getStructure())

    val t3 = jobject.getJSONObjectWithPropertyAlt(listOf("data-exame"))
    println(t3)
}

