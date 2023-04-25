import org.junit.jupiter.api.Test
import kotlin.test.*

class testsLibraryJSON {

    private var jobject = JSONObject()
    private var studentArray = JSONArray()
    private var auxArray = JSONArray()
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
    }

    @Test
    fun testArrayVerification() {
        assertTrue(jobject.verifyArrayEquality("inscritos"))
        assertTrue(jobject.verifyArrayEqualityAlt("inscritos"))

        // Propriedades a menos + Classes Iguais
        auxArray = JSONArray()
        jobject.addElement("auxiliar", auxArray)

        var student5 = JSONObject()
        auxArray.addElement(student5)
        student5.addElement("numero", JSONString("teste"))
        student5.addElement("internacional", JSONBoolean(false))

        var student6 = JSONObject()
        auxArray.addElement(student6)
        student6.addElement("numero", JSONString("teste"))
        student6.addElement("nome", JSONString("André Santos"))
        student6.addElement("internacional", JSONBoolean(false))

        assertFalse(jobject.verifyArrayEquality("auxiliar"))
        assertFalse(jobject.verifyArrayEqualityAlt("auxiliar"))

        // Mesmas propriedades + Classes Diferentes
        var auxArray2 = JSONArray()
        jobject.addElement("auxiliar2", auxArray2)

        var student7 = JSONObject()
        auxArray2.addElement(student7)
        student7.addElement("numero", JSONString("teste"))
        student7.addElement("nome", JSONNumber(10))
        student7.addElement("internacional", JSONBoolean(false))

        var student8 = JSONObject()
        auxArray2.addElement(student8)
        student8.addElement("numero", JSONString("teste"))
        student8.addElement("nome", JSONString("André Santos"))
        student8.addElement("internacional", JSONBoolean(false))

        assertFalse(jobject.verifyArrayEquality("auxiliar2"))
        assertFalse(jobject.verifyArrayEqualityAlt("auxiliar2"))

        // Propriedades diferentes + Classes Diferentes
        var auxArray3 = JSONArray()
        jobject.addElement("auxiliar3", auxArray3)

        var student9 = JSONObject()
        auxArray3.addElement(student9)
        student9.addElement("numero", JSONString("teste"))
        student9.addElement("valido", JSONBoolean(true))
        student9.addElement("internacional", JSONBoolean(false))

        var student10 = JSONObject()
        auxArray3.addElement(student10)
        student10.addElement("numero", JSONString("teste"))
        student10.addElement("nome", JSONString("André Santos"))
        student10.addElement("internacional", JSONBoolean(false))

        assertFalse(jobject.verifyArrayEquality("auxiliar3"))
        assertFalse(jobject.verifyArrayEqualityAlt("auxiliar3"))

        // Array com objetos dentro dos objetos
        var auxArray4 = JSONArray()
        jobject.addElement("auxiliar4", auxArray4)

        var student11 = JSONObject()
        auxArray4.addElement(student11)
        student11.addElement("numero", JSONNumber(93178))
        student11.addElement("nome", JSONString("Afonso Sampaio"))
        student11.addElement("internacional", JSONBoolean(false))
        student11.addElement("extra", student1)

        var student12 = JSONObject()
        auxArray4.addElement(student12)
        student12.addElement("numero", JSONNumber(93179))
        student12.addElement("nome", JSONString("Samuel"))
        student12.addElement("internacional", JSONBoolean(false))
        student12.addElement("extra", student2)

        assertTrue(jobject.verifyArrayEquality("auxiliar4"))
        assertTrue(jobject.verifyArrayEqualityAlt("auxiliar4"))


        // Array com objetos de estruturas diferentes dentro dos objetos
        var auxArray5 = JSONArray()
        jobject.addElement("auxiliar5", auxArray5)

        var student13 = JSONObject()
        auxArray5.addElement(student13)
        student13.addElement("numero", JSONNumber(93178))
        student13.addElement("nome", JSONString("Afonso Sampaio"))
        student13.addElement("internacional", JSONBoolean(false))
        student13.addElement("extra", student1)

        var student14 = JSONObject()
        auxArray5.addElement(student14)
        student14.addElement("numero", JSONNumber(93179))
        student14.addElement("nome", JSONString("Samuel"))
        student14.addElement("internacional", JSONBoolean(false))
        student14.addElement("extra", student5)

        //assertFalse(jobject.verifyArrayEquality("auxiliar5"))
        assertFalse(jobject.verifyArrayEqualityAlt("auxiliar5"))


        // Array com elementos

        val jarray = JSONArray()
        jobject.addElement("differentarray", jarray)
        jarray.addElement(JSONString("E1"))
        jarray.addElement(JSONNumber(1))

        assertFalse(jobject.verifyArrayEquality("differentarray"))
        assertFalse(jobject.verifyArrayEqualityAlt("differentarray"))

        val jarray2 = JSONArray()
        jobject.addElement("simplearray", jarray2)
        jarray2.addElement(JSONNumber(1))
        jarray2.addElement(JSONNumber(2))

        assertTrue(jobject.verifyArrayEquality("simplearray"))
        assertTrue(jobject.verifyArrayEqualityAlt("simplearray"))



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
