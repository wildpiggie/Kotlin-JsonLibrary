import org.junit.jupiter.api.Test
import kotlin.test.*

class TestsJsonLibrary {

    private var jobject = JsonObject()
    private var studentArray = JsonArray()
    private var student1 = JsonObject()
    private var student2 = JsonObject()
    private var student3 = JsonObject()

    /**
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

    /**
     * Used to test if a JSON Object is the same as the one specified in class example.
     *
     * @param objectToTest the JSON Object to be compared to the class example.
     */
    private fun testClassJobjectHierarchy(objectToTest: JsonObject): JsonElement {
        val jsonObjectElements = objectToTest.elements
        assertIs<JsonString>(jsonObjectElements["uc"])
        assertEquals("PA", (jsonObjectElements["uc"] as JsonString).value)
        assertIs<JsonNumber>(jsonObjectElements["ects"])
        assertEquals(6.0, (jsonObjectElements["ects"] as JsonNumber).value)
        assertIs<JsonNull>(jsonObjectElements["data-exame"])
        assertEquals(null, (jsonObjectElements["data-exame"] as JsonNull).value)
        val inscritos = jsonObjectElements["inscritos"]
        assertIs<JsonArray>(inscritos)
        assertTrue(inscritos.elements.all { it is JsonObject })
        val student = inscritos.elements.first()
        assertIs<JsonObject>(student)
        assertTrue(
            student.elements["numero"] is JsonNumber && student.elements["nome"] is JsonString
                    && student.elements["internacional"] is JsonBoolean
        )
        assertEquals(101101, (student.elements["numero"] as JsonNumber).value)
        assertEquals("Dave Farley", (student.elements["nome"] as JsonString).value)
        assertEquals(true, (student.elements["internacional"] as JsonBoolean).value)
        return student
    }

    /**
     * Tests on the hierarchy of created JSON Objects.
     */
    @Test
    fun testHierarchy() {
        testClassJobjectHierarchy(jobject)

        val simpleArray = JsonArray()
        simpleArray.addElement(JsonString("E1"))
        simpleArray.addElement(JsonNumber(1))

        //Adicionar mais testes sobre a estrutura e talvez com a estrutura inteira do exemplo para comparar.
        assertEquals("{\n\t\"numero\" : 26503,\n\t\"nome\" : \"André Santos\",\n\t\"internacional\" : false\n}", student3.getStructure())
        assertEquals("[\n\t\"E1\",\n\t1\n]", simpleArray.getStructure())
    }

    @Test
    fun testSearch() {

        val studentNumbers = mutableListOf<JsonElement>()
        student1.elements["numero"]?.let { studentNumbers.add(it) }
        student2.elements["numero"]?.let { studentNumbers.add(it) }
        student3.elements["numero"]?.let { studentNumbers.add(it) }

        val students = mutableListOf(student1, student2, student3)

        assertEquals(studentNumbers, jobject.getValuesOfProperty("numero"))
        assertEquals(mutableListOf(), jobject.getValuesOfProperty("raiz"))
        assertEquals(mutableListOf(), jobject.getValuesOfProperty(""))
        assertIs<JsonArray>(jobject.getValuesOfProperty("inscritos").first())
        assertIs<List<JsonElement>>((jobject.getValuesOfProperty("inscritos").first() as JsonArray).elements)
        assertEquals(students, (jobject.getValuesOfProperty("inscritos").first() as JsonArray).elements as List<JsonElement> )

        assertEquals(students, jobject.getJsonObjectWithProperties(listOf("numero", "nome")))
        assertEquals(mutableListOf(), jobject.getJsonObjectWithProperties(listOf("numero", "raiz")))
        assertEquals(students, jobject.getJsonObjectWithProperties(listOf("numero", "internacional")))
        assertEquals(mutableListOf(jobject), jobject.getJsonObjectWithProperties(listOf("data-exame")))
        assertEquals(mutableListOf(jobject), jobject.getJsonObjectWithProperties(listOf("inscritos")))
        assertEquals(students, jobject.getJsonObjectWithProperties(listOf("numero", "numero")))
    }

    @Test
    fun testPropertyTypeVerification() {
        assertTrue(jobject.isPropertyOfType("numero", JsonNumber::class))
        assertTrue(jobject.isPropertyOfType("nome", JsonString::class))
        assertTrue(jobject.isPropertyOfType("internacional", JsonBoolean::class))
        assertTrue(jobject.isPropertyOfType("inscritos", JsonArray::class))
        assertTrue(jobject.isPropertyOfType("data-exame", JsonNull::class))
        assertFalse(jobject.isPropertyOfType("internacional", JsonArray::class))
        assertFalse(jobject.isPropertyOfType("numero", JsonString::class))
        assertTrue(jobject.isPropertyOfType("inexistente", JsonString::class))

        val studentWithInvalidPropertyType = JsonObject()
        studentArray.addElement(studentWithInvalidPropertyType)
        studentWithInvalidPropertyType.addElement("numero", JsonString("teste"))
        studentWithInvalidPropertyType.addElement("nome", JsonString("André Santos"))
        studentWithInvalidPropertyType.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isPropertyOfType("numero", JsonNumber::class))
    }

    @Test
    fun testArrayStructureVerification() {

        assertTrue(jobject.isArrayStructureHomogenousShallow("inscritos"))
        assertTrue(jobject.isArrayStructureHomogenousDeep("inscritos"))

        // Array com elementos
        val nonHomogenousArray = JsonArray()
        jobject.addElement("nonHomogenousArray", nonHomogenousArray)
        nonHomogenousArray.addElement(JsonString("E1"))
        nonHomogenousArray.addElement(JsonNumber(1))

        assertFalse(jobject.isArrayStructureHomogenousShallow("nonHomogenousArray"))
        assertFalse(jobject.isArrayStructureHomogenousDeep("nonHomogenousArray"))

        val homogenousArray = JsonArray()
        jobject.addElement("homogenousArray", homogenousArray)
        homogenousArray.addElement(JsonNumber(1))
        homogenousArray.addElement(JsonNumber(2))

        assertTrue(jobject.isArrayStructureHomogenousShallow("homogenousArray"))
        assertTrue(jobject.isArrayStructureHomogenousDeep("homogenousArray"))

        // Array com elementos e objetos
        val mixedArray = JsonArray()
        jobject.addElement("mixedArray", mixedArray)
        mixedArray.addElement(JsonNumber(1))
        mixedArray.addElement(student1)

        assertFalse(jobject.isArrayStructureHomogenousShallow("mixedArray"))
        assertFalse(jobject.isArrayStructureHomogenousDeep("mixedArray"))

        // propriedade nao correspondente a um array
        assertTrue(jobject.isArrayStructureHomogenousShallow("uc"))
        assertTrue(jobject.isArrayStructureHomogenousDeep("uc"))

        assertTrue(jobject.isArrayStructureHomogenousShallow("naoexiste"))
        assertTrue(jobject.isArrayStructureHomogenousDeep("naoexiste"))

        // Propriedades a menos + Classes Iguais
        val arrayWithMissingProperty = JsonArray()
        jobject.addElement("testArrayWithMissingProperty", arrayWithMissingProperty)

        arrayWithMissingProperty.addElement(student1)

        val studentWithMissingProperty = JsonObject()
        arrayWithMissingProperty.addElement(studentWithMissingProperty)
        studentWithMissingProperty.addElement("numero", JsonString("teste"))
        studentWithMissingProperty.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isArrayStructureHomogenousShallow("testArrayWithMissingProperty"))
        assertFalse(jobject.isArrayStructureHomogenousDeep("testArrayWithMissingProperty"))

        // Mesmas propriedades + Classes Diferentes
        val ArrayWithDifferingTypes = JsonArray()
        jobject.addElement("testArrayWithDifferingTypes", ArrayWithDifferingTypes)

        val studentWithDifferentType = JsonObject()
        ArrayWithDifferingTypes.addElement(studentWithDifferentType)
        studentWithDifferentType.addElement("numero", JsonString("teste"))
        studentWithDifferentType.addElement("nome", JsonString("nome"))
        studentWithDifferentType.addElement("internacional", JsonBoolean(false))

        ArrayWithDifferingTypes.addElement(student1)

        assertFalse(jobject.isArrayStructureHomogenousShallow("testArrayWithDifferingTypes"))
        assertFalse(jobject.isArrayStructureHomogenousDeep("testArrayWithDifferingTypes"))

        // Propriedades diferentes + Classes Diferentes
        val arrayWithMissingPropertiesAndDifferingNames = JsonArray()
        jobject.addElement("arrayWithMissingPropertiesAndDifferingNames", arrayWithMissingPropertiesAndDifferingNames)

        arrayWithMissingPropertiesAndDifferingNames.addElement(student1)

        val studentMissingNameAndDifferingType = JsonObject()
        arrayWithMissingPropertiesAndDifferingNames.addElement(studentMissingNameAndDifferingType)
        studentMissingNameAndDifferingType.addElement("numero", JsonString("teste"))
        studentMissingNameAndDifferingType.addElement("valido", JsonBoolean(true))
        studentMissingNameAndDifferingType.addElement("internacional", JsonBoolean(false))

        assertFalse(jobject.isArrayStructureHomogenousShallow("arrayWithMissingPropertiesAndDifferingNames"))
        assertFalse(jobject.isArrayStructureHomogenousDeep("arrayWithMissingPropertiesAndDifferingNames"))

        // Array com objetos dentro dos objetos
        val arrayWithObjectsWithinOfObjects = JsonArray()
        jobject.addElement("arrayWithObjectsInsideOfObjects", arrayWithObjectsWithinOfObjects)

        arrayWithObjectsWithinOfObjects.addElement(student1)
        arrayWithObjectsWithinOfObjects.addElement(student2)
        student1.addElement("extra", student3)
        student2.addElement("extra", student3)

        assertTrue(jobject.isArrayStructureHomogenousShallow("arrayWithObjectsInsideOfObjects"))
        assertTrue(jobject.isArrayStructureHomogenousDeep("arrayWithObjectsInsideOfObjects"))

        // Array com objetos de estruturas diferentes dentro dos objetos
        val arrayWithDifferingObjectsWithinObjects = JsonArray()
        jobject.addElement("arrayWithObjectsDifferingObjectsWithinObjects", arrayWithDifferingObjectsWithinObjects)

        arrayWithDifferingObjectsWithinObjects.addElement(student1)

        val studentMissingPropertyStudent = JsonObject()
        studentMissingPropertyStudent.addElement("numero", JsonNumber(101101))
        studentMissingPropertyStudent.addElement("nome", JsonString("Dave Farley"))
        studentMissingPropertyStudent.addElement("internacional", JsonBoolean(true))
        studentMissingPropertyStudent.addElement("extra", studentWithMissingProperty)

        arrayWithDifferingObjectsWithinObjects.addElement(studentMissingPropertyStudent)

        assertTrue(jobject.isArrayStructureHomogenousShallow("arrayWithObjectsDifferingObjectsWithinObjects"))
        assertFalse(jobject.isArrayStructureHomogenousDeep("arrayWithObjectsDifferingObjectsWithinObjects"))
    }

    /**
     * Tests the inference via reflection using the original test hierarchy
     * as well as a custom hierarchy to verify as many scenarios as possible.
     */
    @Test
    fun testInference() {
        val classObject = ClassObject(UC.PA, 6.0, null)
        classObject.addInscrito(StudentObject(101101, "Dave Farley", true))
        classObject.addInscrito(StudentObject(101102, "Martin Fowler", true))
        classObject.addInscrito(StudentObject(26503, "André Santos", false))

        val jsonClassObject = classObject.toJson()
        assertIs<JsonObject>(jsonClassObject)

        testClassJobjectHierarchy(jsonClassObject)

        val customClassObject = CustomClassObject(
                StudentObject(0, "A", true),
                listOf(1, StudentObject(1, "S", false), listOf(1, 2, 3)),
                mapOf("One point five" to 1.5, "Two point two" to 2.2),
                1, true, 'x', "stringValue", UC.PGMV, "excluded", false, 99)

        val customClassObjectJson = customClassObject.toJson()
        assertIs<JsonObject>(customClassObjectJson)

        val customJsonElements = customClassObjectJson.elements

        assertIs<JsonObject>(customJsonElements["student"])
        val list = customJsonElements["list"]
        assertIs<JsonArray>(list)
        assertTrue(list.elements[0] is JsonNumber && list.elements[1] is JsonObject && list.elements[2] is JsonArray)
        val mapObject = customJsonElements["map"]
        assertIs<JsonObject>(mapObject)
        assertTrue(mapObject.elements.all { it.value is JsonNumber })
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