import jsonLibrary.JsonAsString
import jsonLibrary.JsonExclude
import jsonLibrary.JsonName

/*
 * Custom classes and enums to be used in testing.
 */

enum class UC {
    PA, EPCS, PGMV, ICO, GCCO
}

data class StudentObject(
    val numero: Number,
    val nome: String,
    val internacional: Boolean
)

data class ClassObject(
    val uc: UC,
    val ects: Number,
    @JsonName("data-exame")
    val dataExame: String?,
    val inscritos: MutableCollection<StudentObject> = mutableListOf()
) {
    fun addInscrito(value: StudentObject) = inscritos.add(value)
}

data class CustomClassObject(
    val student: StudentObject,
    val list: List<Any>,
    val map: Map<*, *>,
    val number: Number,
    val boolean: Boolean,
    val character: Char,
    val string: String,
    val enum: UC,
    @JsonExclude
    val excludedString: String = "excluded",
    @JsonName("lie")
    val truth: Boolean,
    @JsonAsString
    val numberAsString: Int
)
