import org.junit.jupiter.api.Test
import kotlin.test.*

class testsLibraryJSON {

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
        val jarray = JSONArray()
        jarray.addElement(JSONString("E1"))
        jarray.addElement(JSONNumber(1))

        assertEquals("{numero=101101, nome=\"Dave Farley\", internacional=true}", student1.toString())
        assertEquals("[\"E1\", 1]", jarray.toString())
        assertEquals("{uc=\"PA\", ects=6.0, data-exame=null, inscritos=[{numero=101101, nome=\"Dave Farley\", internacional=true}, " +
                "{numero=101102, nome=\"Martin Fowler\", internacional=true}, {numero=26503, nome=\"André Santos\", internacional=false}]}", jobject.toString())
        assertEquals("{\n\t\"numero\" : 26503,\n\t\"nome\" : \"André Santos\",\n\t\"internacional\" : false\n}", student3.getStructure())
        assertEquals("[\n\t\"E1\",\n\t1\n]", jarray.getStructure())
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
        assertIs<JSONArray>(jobject.getValuesByProperty("inscritos")[0])
        assertIs<List<JSONElement>>((jobject.getValuesByProperty("inscritos")[0] as JSONArray).elements)
        assertEquals(students, (jobject.getValuesByProperty("inscritos")[0] as JSONArray).elements as List<JSONElement> )

        assertEquals(students, jobject.getJSONObjectWithProperty(listOf("numero", "nome")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithProperty(listOf("numero", "raiz")))
        assertEquals(students, jobject.getJSONObjectWithProperty(listOf("numero", "internacional")))
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithProperty(listOf("data-exame")))
        assertEquals(mutableListOf(jobject), jobject.getJSONObjectWithProperty(listOf("inscritos")))
        assertEquals(mutableListOf(), jobject.getJSONObjectWithProperty(listOf()))
        assertEquals(students, jobject.getJSONObjectWithProperty(listOf("numero", "numero")))

    }

    @Test
    fun testVerifications() {
        var student4 = JSONObject()
        student4.addElement("numero", JSONString("teste"))
        student4.addElement("nome", JSONString("André Santos"))
        student4.addElement("internacional", JSONBoolean(false))

        assertFalse(student4.verifyStructure("numero", JSONNumber::class))
        assertTrue(jobject.verifyStructure("numero", JSONNumber::class))
        assertTrue(jobject.verifyStructure("nome", JSONString::class))
        assertTrue(jobject.verifyStructure("internacional", JSONBoolean::class))
        assertTrue(jobject.verifyStructure("inscritos", JSONArray::class))
        assertTrue(jobject.verifyStructure("data-exame", JSONNull::class))
        assertFalse(jobject.verifyStructure("internacional", JSONArray::class))
        assertFalse(jobject.verifyStructure("numero", JSONString::class))

        //assertTrue(jobject.verifyArrayEquality("inscritos"))
    }

    @Test
    fun testInference() {
        val classObject: ClassObject = ClassObject(UC.PA, 6.0, null)
        classObject.addInscrito(StudentObject(101101, "Dave Farley", true))
        classObject.addInscrito(StudentObject(101102, "Martin Fowler", true))
        classObject.addInscrito(StudentObject(26503, "André Santos", false))


        //assertEquals(jobject.getStructure(), TODO())
    }
}
