import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class testsLibraryJSON {

    @Test
    fun testHierarchy() {
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

        /**val jobject3 = JSONObject()
        jarray.addElement(jobject3)
        jobject3.addElement("numero", JSONNumber(101102))
        jobject3.addElement("nome", JSONString("Martin Fowler"))
        jobject3.addElement("internacional", JSONBoolean(true))

        val jobject4 = JSONObject()
        jarray.addElement(jobject4)
        jobject4.addElement("numero", JSONNumber(26503))
        jobject4.addElement("nome", JSONString("André Santo"))
        jobject4.addElement("internacional", JSONBoolean(false))
         **/

        val jarray2 = JSONArray()
        jarray2.addElement(JSONString("E1"))
        jarray2.addElement(JSONNumber(1))

        assertEquals("{numero=101101, nome=\"Dave Farley\", internacional=true}", jobject2.toString())
        assertEquals("[\"E1\", 1]", jarray2.toString())
        assertEquals("{uc=\"PA\", ects=6.0, data-exame=null, inscritos=[{numero=101101, nome=\"Dave Farley\", internacional=true}]}", jobject.toString())
    }

    @Test
    fun testSearch() {
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
        //jobject3.addElement("internacional", JSONBoolean(true))

        /**

        val jobject4 = JSONObject()
        jarray.addElement(jobject4)
        jobject4.addElement("numero", JSONNumber(26503))
        jobject4.addElement("nome", JSONString("André Santo"))
        jobject4.addElement("internacional", JSONBoolean(false))
         **/

        //assertEquals(mutableListOf(JSONNumber(101101), JSONNumber(101102)), jobject.getValuesByProperty("numero")) // da errado porque ao comparar um objecto com o seu valor da erro -> Soluçao seria ter uma lista de Any? e meter la os jsonleaf.values em vez do json leaf em questao
        //assertEquals(mutableListOf(JSONString("PA")), jobject.getValuesByProperty("uc"))
        assertEquals(mutableListOf(jarray), jobject.getValuesByProperty("inscritos"))
        assertEquals(mutableListOf(), jobject.getValuesByProperty("raiz"))
        assertEquals(mutableListOf(), jobject.getValuesByProperty(""))


        /**
        assertEquals(resultT2, jobject.getJSONObjectWithProperty(listOf("numero", "nome")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithProperty(listOf("numero", "raiz")))
        assertEquals(mutableListOf(jobject2), jobject.getJSONObjectWithProperty(listOf("numero", "internacional")))
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithProperty(listOf("data-exame"))) // devolve vazio ??
        assertEquals(resultT2, jobject.getJSONObjectWithProperty(listOf("numero", "numero"))) // ter em consideraçao este caso? -> este problema resolve se ao em vez de comparar o counter com o tamanho da lista, comparar com o numero de elementos diferentes da lista
        **/

        assertEquals(mutableListOf(jobject2, jobject3), jobject.getJSONObjectWithPropertyAlt(listOf("numero", "nome")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithPropertyAlt(listOf("numero", "raiz")))
        assertEquals(mutableListOf(jobject2), jobject.getJSONObjectWithPropertyAlt(listOf("numero", "internacional")))
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithPropertyAlt(listOf("data-exame")))
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithPropertyAlt(listOf("inscritos")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithPropertyAlt(listOf()))
        //assertEquals(resultT2, jobject.getJSONObjectWithProperty2(listOf("numero", "numero"))) -> fzr a verificaçao dif
    }
}
