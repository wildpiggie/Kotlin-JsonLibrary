import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestsLibraryJSON {

    private var jobject = JSONObject()
    private var studentArray = JSONArray()
    private var student1 = JSONObject()
    private var student2 = JSONObject()
    private var student3 = JSONObject()

    @BeforeTest
    fun createHierarchy() {
        jobject = JSONObject()
        jobject.addElement("uc", JSONString("PA"))
        jobject.addElement("ects", JSONNumber(6.0))
        jobject.addElement("data-exame", JSONNull())

        studentArray = JSONArray()
        jobject.addElement("inscritos", studentArray)

        student1 = JSONObject()
        studentArray.addElement(student1)
        student1.addElement("numero", JSONNumber(101101))
        student1.addElement("nome", JSONString("Dave Farley"))
        student1.addElement("internacional", JSONBoolean(true))

        student2 = JSONObject()
        studentArray.addElement(student2)
        student2.addElement("numero", JSONNumber(101102))
        student2.addElement("nome", JSONString("Martin Fowler"))
        student2.addElement("internacional", JSONBoolean(true))

        student3 = JSONObject()
        studentArray.addElement(student3)
        student3.addElement("numero", JSONNumber(26503))
        student3.addElement("nome", JSONString("André Santos"))
        student3.addElement("internacional", JSONBoolean(false))
    }

    @Test
    fun testHierarchy() {
        val jarray2 = JSONArray()
        jarray2.addElement(JSONString("E1"))
        jarray2.addElement(JSONNumber(1))

        assertEquals("{numero=101101, nome=\"Dave Farley\", internacional=true}", student1.toString())
        assertEquals("[\"E1\", 1]", jarray2.toString())
        assertEquals("{uc=\"PA\", ects=6.0, data-exame=null, inscritos=[{numero=101101, nome=\"Dave Farley\", internacional=true}, " +
                "{numero=101102, nome=\"Martin Fowler\", internacional=true}, {numero=26503, nome=\"André Santos\", internacional=false}]}", jobject.toString())
    }

    @Test
    fun testSearch() {

        val jarray2 = JSONArray()
        jarray2.addElement(JSONString("E1"))
        jarray2.addElement(JSONNumber(1))

        val result = mutableListOf<JSONElement>()
        student1.elements["numero"]?.let { result.add(it) }
        student2.elements["numero"]?.let { result.add(it) }
        student3.elements["numero"]?.let { result.add(it) }

        val students = mutableListOf(student1, student2, student3)

        assertEquals(result, jobject.getValuesByProperty("numero"))
        assertEquals(mutableListOf(), jobject.getValuesByProperty("raiz"))
        assertEquals(mutableListOf(), jobject.getValuesByProperty(""))


        //assertEquals(students, jobject.getJSONObjectWithProperty(listOf("numero", "nome")))
        //assertEquals(mutableListOf(), jobject.getJSONObjectWithProperty(listOf("numero", "raiz")))
        //assertEquals(students, jobject.getJSONObjectWithProperty(listOf("numero", "internacional")))
        /*
        assertEquals(mutableListOf(jobject2, jobject3), jobject.getJSONObjectWithPropertyAlt(listOf("numero", "nome")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithPropertyAlt(listOf("numero", "raiz")))
        assertEquals(mutableListOf(jobject2), jobject.getJSONObjectWithPropertyAlt(listOf("numero", "internacional")))
        */
        
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithPropertyAlt(listOf("data-exame")))
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithPropertyAlt(listOf("inscritos")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithPropertyAlt(listOf()))
        //assertEquals(resultT2, jobject.getJSONObjectWithProperty2(listOf("numero", "numero"))) -> fzr a verificaçao dif
    }

    @Test
    fun testInference() {
        val classObject = ClassObject(UC.PA, 6.0, null)
        classObject.addInscrito(StudentObject(101101, "Dave Farley", true))
        classObject.addInscrito(StudentObject(101102, "Martin Fowler", true))
        classObject.addInscrito(StudentObject(26503, "André Santos", false))

        val jsonClassObject: JSONObject = classObject.toJson()
        assertEquals(jobject.getStructure(), jsonClassObject.getStructure())

        val customClassObject = CustomClassObject(
                StudentObject(0, "A", true),
                listOf(1, StudentObject(1, "S", false), listOf(1, 2, 3)),
                mapOf("One point five" to 1.5, "Two point two" to 2.2),
                1, true, 'x', "stringValue", UC.PGMV, "excluded", false, 99)

        val customJsonElements = customClassObject.toJson().elements

        assertIs<JSONObject>(customJsonElements["student"])
        val list = customJsonElements["list"]
        assertIs<JSONArray>(list)
        assertTrue(list.elements[0] is JSONNumber && list.elements[1] is JSONObject && list.elements[2] is JSONArray)
        val mapObject = customJsonElements["map"]
        assertIs<JSONObject>(mapObject)
        assertTrue(mapObject.elements["One point five"] is JSONNumber && mapObject.elements["Two point two"] is JSONNumber)
        assertIs<JSONNumber>(customJsonElements["number"])
        assertEquals("1", customJsonElements["number"].toString()) //quermos fazer isto para todos? Ou colocar as variáveis como públicas para poderem ser vistas
        assertIs<JSONBoolean>(customJsonElements["boolean"])
        assertIs<JSONString>(customJsonElements["character"])
        assertIs<JSONString>(customJsonElements["string"])
        assertIs<JSONString>(customJsonElements["enum"])
        assertTrue(!customJsonElements.contains("excluded"))
        assertTrue(!customJsonElements.contains("truth") && customJsonElements.contains("lie"))
        assertIs<JSONString>(customJsonElements["numberAsString"])
        assertEquals("\"99\"", customJsonElements["numberAsString"].toString())
    }
}
