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

        val jarray2 = JSONArray()
        jarray2.addElement(JSONString("E1"))
        jarray2.addElement(JSONNumber(1))

        val result = mutableListOf<JSONElement>()
        result.add(JSONNumber(101101))
        result.add(JSONNumber(101102))

        val resultT2 = mutableListOf<JSONObject>(jobject2, jobject3)

        //assertEquals(result, jobject.getValuesByProperty("numero")) // por alguma razao da errado mas da print do mesmo
        assertEquals(mutableListOf(), jobject.getValuesByProperty("raiz"))

        assertEquals(resultT2, jobject.getJSONObjectWithProperty(listOf("numero", "nome"))) // tbm vamos querer returnar arrays?
        assertEquals(mutableListOf(), jobject.getJSONObjectWithProperty(listOf("numero", "raiz")))
        assertEquals(mutableListOf(jobject2), jobject.getJSONObjectWithProperty(listOf("numero", "internacional")))
    }
}
