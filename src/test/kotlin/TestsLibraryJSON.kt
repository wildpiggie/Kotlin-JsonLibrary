import org.junit.jupiter.api.Test
import kotlin.test.*

class TestsLibraryJSON {

    private var jobject = JsonObject()
    private var studentArray = JsonArray()
    private var auxArray = JsonArray()
    private var student1 = JsonObject()
    private var student2 = JsonObject()
    private var student3 = JsonObject()

    /*
     * Creates an example JSON Element hierarchy to be used in testing,
     * Executed before each test to guarantee consistent results.
     */
    @BeforeTest
    fun createHierarchy() {
        jobject = JsonObject()
        jobject.addElement("uc", JsonString("PA"))
        jobject.addElement("ects", JsonNumber(6.0))
        jobject.addElement("data-exame", JsonNull())

        studentArray = JsonArray()
        jobject.addElement("inscritos", studentArray)

        student1 = JsonObject()
        studentArray.addElement(student1)
        student1.addElement("numero", JsonNumber(101101))
        student1.addElement("nome", JsonString("Dave Farley"))
        student1.addElement("internacional", JsonBoolean(true))

        student2 = JsonObject()
        studentArray.addElement(student2)
        student2.addElement("numero", JsonNumber(101102))
        student2.addElement("nome", JsonString("Martin Fowler"))
        student2.addElement("internacional", JsonBoolean(true))

        student3 = JsonObject()
        studentArray.addElement(student3)
        student3.addElement("numero", JsonNumber(26503))
        student3.addElement("nome", JsonString("André Santos"))
        student3.addElement("internacional", JsonBoolean(false))
    }

    @Test
    fun testHierarchy() {
        val jarray = JsonArray()
        jarray.addElement(JsonString("E1"))
        jarray.addElement(JsonNumber(1))

        assertEquals("{numero=101101, nome=\"Dave Farley\", internacional=true}", student1.toString())
        assertEquals("[\"E1\", 1]", jarray.toString())
        assertEquals("{uc=\"PA\", ects=6.0, data-exame=null, inscritos=[{numero=101101, nome=\"Dave Farley\", internacional=true}, " +
                "{numero=101102, nome=\"Martin Fowler\", internacional=true}, {numero=26503, nome=\"André Santos\", internacional=false}]}", jobject.toString())
        assertEquals("{\n\t\"numero\" : 26503,\n\t\"nome\" : \"André Santos\",\n\t\"internacional\" : false\n}", student3.getStructure())
        assertEquals("[\n\t\"E1\",\n\t1\n]", jarray.getStructure())
    }

    @Test
    fun testSearch() {
        val jarray2 = JsonArray()
        jarray2.addElement(JsonString("E1"))
        jarray2.addElement(JsonNumber(1))

        val result = mutableListOf<JsonElement>()
        student1.elements["numero"]?.let { result.add(it) }
        student2.elements["numero"]?.let { result.add(it) }
        student3.elements["numero"]?.let { result.add(it) }

        val students = mutableListOf(student1, student2, student3)

        assertEquals(result, jobject.getValuesByProperty("numero"))
        assertEquals(mutableListOf(), jobject.getValuesByProperty("raiz"))
        assertEquals(mutableListOf(), jobject.getValuesByProperty(""))
        assertIs<JsonArray>(jobject.getValuesByProperty("inscritos")[0])
        assertIs<List<JsonElement>>((jobject.getValuesByProperty("inscritos")[0] as JsonArray).elements)
        assertEquals(students, (jobject.getValuesByProperty("inscritos")[0] as JsonArray).elements as List<JsonElement> )

        assertEquals(students, jobject.getJsonObjectWithProperty(listOf("numero", "nome")))
        assertEquals(mutableListOf(), jobject.getJsonObjectWithProperty(listOf("numero", "raiz")))
        assertEquals(students, jobject.getJsonObjectWithProperty(listOf("numero", "internacional")))
        assertEquals(mutableListOf(jobject), jobject.getJsonObjectWithProperty(listOf("data-exame")))
        assertEquals(mutableListOf(jobject), jobject.getJsonObjectWithProperty(listOf("inscritos")))
        assertEquals(mutableListOf(), jobject.getJsonObjectWithProperty(listOf()))
        assertEquals(students, jobject.getJsonObjectWithProperty(listOf("numero", "numero")))

    }

    @Test
    fun testVerifications() {
        assertTrue(jobject.isPropertyOfType("numero", JsonNumber::class))
        assertTrue(jobject.isPropertyOfType("nome", JsonString::class))
        assertTrue(jobject.isPropertyOfType("internacional", JsonBoolean::class))
        assertTrue(jobject.isPropertyOfType("inscritos", JsonArray::class))
        assertTrue(jobject.isPropertyOfType("data-exame", JsonNull::class))
        assertFalse(jobject.isPropertyOfType("internacional", JsonArray::class))
        assertFalse(jobject.isPropertyOfType("numero", JsonString::class))

        assertTrue(jobject.isPropertyOfType("inexistente", JsonString::class))

        var student4 = JsonObject()
        studentArray.addElement(student4)
        student4.addElement("numero", JsonString("teste"))
        student4.addElement("nome", JsonString("André Santos"))
        student4.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isPropertyOfType("numero", JsonNumber::class))

    }

    @Test
    fun testArrayVerification() {
        assertTrue(jobject.isArrayStructureHomogenous("inscritos"))
        assertTrue(jobject.isArrayStructureHomogenousAlt("inscritos"))

        // Propriedades a menos + Classes Iguais
        auxArray = JsonArray()
        jobject.addElement("auxiliar", auxArray)

        var student5 = JsonObject()
        auxArray.addElement(student5)
        student5.addElement("numero", JsonString("teste"))
        student5.addElement("internacional", JsonBoolean(false))

        var student6 = JsonObject()
        auxArray.addElement(student6)
        student6.addElement("numero", JsonString("teste"))
        student6.addElement("nome", JsonString("André Santos"))
        student6.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isArrayStructureHomogenous("auxiliar"))
        assertFalse(jobject.isArrayStructureHomogenousAlt("auxiliar"))

        // Mesmas propriedades + Classes Diferentes
        var auxArray2 = JsonArray()
        jobject.addElement("auxiliar2", auxArray2)

        var student7 = JsonObject()
        auxArray2.addElement(student7)
        student7.addElement("numero", JsonString("teste"))
        student7.addElement("nome", JsonNumber(10))
        student7.addElement("internacional", JsonBoolean(false))

        var student8 = JsonObject()
        auxArray2.addElement(student8)
        student8.addElement("numero", JsonString("teste"))
        student8.addElement("nome", JsonString("André Santos"))
        student8.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isArrayStructureHomogenous("auxiliar2"))
        assertFalse(jobject.isArrayStructureHomogenousAlt("auxiliar2"))

        // Propriedades diferentes + Classes Diferentes
        var auxArray3 = JsonArray()
        jobject.addElement("auxiliar3", auxArray3)

        var student9 = JsonObject()
        auxArray3.addElement(student9)
        student9.addElement("numero", JsonString("teste"))
        student9.addElement("valido", JsonBoolean(true))
        student9.addElement("internacional", JsonBoolean(false))

        var student10 = JsonObject()
        auxArray3.addElement(student10)
        student10.addElement("numero", JsonString("teste"))
        student10.addElement("nome", JsonString("André Santos"))
        student10.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isArrayStructureHomogenous("auxiliar3"))
        assertFalse(jobject.isArrayStructureHomogenousAlt("auxiliar3"))

        // Array com objetos dentro dos objetos
        var auxArray4 = JsonArray()
        jobject.addElement("auxiliar4", auxArray4)

        var student11 = JsonObject()
        auxArray4.addElement(student11)
        student11.addElement("numero", JsonNumber(93178))
        student11.addElement("nome", JsonString("Afonso Sampaio"))
        student11.addElement("internacional", JsonBoolean(false))
        student11.addElement("extra", student1)

        var student12 = JsonObject()
        auxArray4.addElement(student12)
        student12.addElement("numero", JsonNumber(93179))
        student12.addElement("nome", JsonString("Samuel"))
        student12.addElement("internacional", JsonBoolean(false))
        student12.addElement("extra", student2)

        assertTrue(jobject.isArrayStructureHomogenous("auxiliar4"))
        assertTrue(jobject.isArrayStructureHomogenousAlt("auxiliar4"))


        // Array com objetos de estruturas diferentes dentro dos objetos
        var auxArray5 = JsonArray()
        jobject.addElement("auxiliar5", auxArray5)

        var student13 = JsonObject()
        auxArray5.addElement(student13)
        student13.addElement("numero", JsonNumber(93178))
        student13.addElement("nome", JsonString("Afonso Sampaio"))
        student13.addElement("internacional", JsonBoolean(false))
        student13.addElement("extra", student1)

        var student14 = JsonObject()
        auxArray5.addElement(student14)
        student14.addElement("numero", JsonNumber(93179))
        student14.addElement("nome", JsonString("Samuel"))
        student14.addElement("internacional", JsonBoolean(false))
        student14.addElement("extra", student5)

        //assertFalse(jobject.verifyArrayEquality("auxiliar5"))
        assertFalse(jobject.isArrayStructureHomogenousAlt("auxiliar5"))


        // Array com elementos
        val jarray = JsonArray()
        jobject.addElement("differentarray", jarray)
        jarray.addElement(JsonString("E1"))
        jarray.addElement(JsonNumber(1))

        assertFalse(jobject.isArrayStructureHomogenous("differentarray"))
        assertFalse(jobject.isArrayStructureHomogenousAlt("differentarray"))

        val jarray2 = JsonArray()
        jobject.addElement("simplearray", jarray2)
        jarray2.addElement(JsonNumber(1))
        jarray2.addElement(JsonNumber(2))

        assertTrue(jobject.isArrayStructureHomogenous("simplearray"))
        assertTrue(jobject.isArrayStructureHomogenousAlt("simplearray"))

        // Array com elementos e objetos
        val jarray3 = JsonArray()
        jobject.addElement("mixedarray", jarray3)
        jarray3.addElement(JsonNumber(1))
        jarray3.addElement(student1)

        assertFalse(jobject.isArrayStructureHomogenous("mixedarray"))
        assertFalse(jobject.isArrayStructureHomogenousAlt("mixedarray"))

        // propriedade nao correspondente a um array
        assertTrue(jobject.isArrayStructureHomogenous("uc"))
        assertTrue(jobject.isArrayStructureHomogenousAlt("uc"))

        assertTrue(jobject.isArrayStructureHomogenous("naoexiste"))
        assertTrue(jobject.isArrayStructureHomogenousAlt("naoexiste"))
    }

    /*
     * Tests the inference via reflection using the original test hierarchy
     * as well as a custom hierarchy to verify as many scenarios as possible.
     */
    @Test
    fun testInference() {
        val classObject = ClassObject(UC.PA, 6.0, null)
        classObject.addInscrito(StudentObject(101101, "Dave Farley", true))
        classObject.addInscrito(StudentObject(101102, "Martin Fowler", true))
        classObject.addInscrito(StudentObject(26503, "André Santos", false))

        val jsonClassObject: JsonObject = classObject.toJson()
        assertEquals(jobject.getStructure(), jsonClassObject.getStructure())

        val customClassObject = CustomClassObject(
                StudentObject(0, "A", true),
                listOf(1, StudentObject(1, "S", false), listOf(1, 2, 3)),
                mapOf("One point five" to 1.5, "Two point two" to 2.2),
                1, true, 'x', "stringValue", UC.PGMV, "excluded", false, 99)

        val customJsonElements = customClassObject.toJson().elements

        assertIs<JsonObject>(customJsonElements["student"])
        val list = customJsonElements["list"]
        assertIs<JsonArray>(list)
        assertTrue(list.elements[0] is JsonNumber && list.elements[1] is JsonObject && list.elements[2] is JsonArray)
        val mapObject = customJsonElements["map"]
        assertIs<JsonObject>(mapObject)
        assertTrue(mapObject.elements["One point five"] is JsonNumber && mapObject.elements["Two point two"] is JsonNumber)
        assertIs<JsonNumber>(customJsonElements["number"])
        assertEquals(1, (customJsonElements["number"] as JsonNumber).value)
        assertIs<JsonBoolean>(customJsonElements["boolean"])
        assertEquals(true, (customJsonElements["boolean"] as JsonBoolean).value)
        assertIs<JsonString>(customJsonElements["character"])
        assertEquals("x", (customJsonElements["character"] as JsonString).value)
        assertIs<JsonString>(customJsonElements["string"])
        assertEquals("stringValue", (customJsonElements["string"] as JsonString).value)
        assertIs<JsonString>(customJsonElements["enum"])
        assertEquals("PGMV", (customJsonElements["enum"] as JsonString).value)
        assertTrue(!customJsonElements.contains("excluded"))
        assertTrue(!customJsonElements.contains("truth") && customJsonElements.contains("lie"))
        assertIs<JsonString>(customJsonElements["numberAsString"])
        assertEquals("\"99\"", (customJsonElements["numberAsString"] as JsonString).toString())
    }
}
