# Kotlin JsonLibrary

**A kotlin library for the creation and manipulation of JSON Objects, instantiation through reflection, and textual serialization**

[JSON](https://www.json.org/json-en.html) (JavaScript Object Notation) is a lightweight data-interchange format. It is easy for humans to read and write. 
It is easy for machines to parse and generate.

JSON Example:
```json
{
  "uc": "PA",
  "ects" : 6.0,
  "data-exame" : null,
  "inscritos" : [
    {
      "numero" : 101101,
      "nome" : "Dave Farley",
      "internacional" : true
    },
    {
      "numero" : 101102,
      "nome" : "Martin Fowler",
      "internacional" : true
    },
    {
      "numero" : 26503,
      "nome" : "André Santos",
      "internacional" : false
    }
  ]
}
```

## Model

To represent JSON data, the library follows a strongly-typed approach with classes (prefixed with "Json") representing the different types of JSON elements:

- Object
- Array
- String
- Number
- Boolean
- Null

Note that the names of JSON elements are only stored in their corresponding JSON Objects.

There are two distinct types of JSON elements in the library: Leaf and Composite. While a Leaf contains a value of a certain type (String, Number, Boolean, Null) 
the Composites (Object, Array) contain references to other JSON elements. In the case of JSON Objects each element has an associated name.

## Usage

To instantiate a JSON object such as the example above, we start by creating an empty JSON Object. 
We then instantiate each of the values inside of this object, before adding them with their desired names: 
```kotlin
val object = JsonObject()

val ucString = JsonString("PA")
val ectsNumber = JsonNumber(6.0)
val dataNull = JsonNull()

object.addElement("uc", ucString)
object.addElement("ects", ectsNumber)
object.addElement("data-exame", dataNull)
```

The creation of JSON Arrays is done in the same way as the JSON Objects with the only difference being that no name is given to the values:
```kotlin
val array = JsonArray()
object.addElement("inscritos", array)

//Creating an object for the array: 
val student = JsonObject()
student.addElement("numero", JsonNumber(101101))
student.addElement("nome", JsonString("Dave Farley"))
student.addElement("internacional", JsonBoolean(true))

//Adding the object to the array
array.addElement(student)
```

To obtain the corresponding JSON format structure simply use the `getStructure()` method. This will return a string containing the represented JSON structure.

### Instantiation Through Reflection

It is possible to instantiate JSON Objects through reflection. To do so, simply use the `toJson()` method with any instance ("instanceToInfer" in this example):
```kotlin
val jsonElement: JsonElement = instanceToInfer.toJson()
```

While this method can be used with any object, it is most useful when used with a data class, to obtain its JSON representation.
To alter the properties of the instantiation one of the library's annotations may be used:
- `@JsonExclude` : Exclude a property
- `@JsonName(val name: String)` : Customize the element's name
- `@JsonAsString` : Force type to be considered string

Example:
```kotlin
data class exampleClass(
    @JsonExclude
    val excludedString: String,
    @JsonName("Boolean")
    val booleanValue: Boolean,
    @JsonAsString
    val numberAsString: Int
)
```

For the instance given by `exampleClass("excluded", true, 6)`, using the `toJson()` and `getStructure()` methods, we obtain:
```json
{
  "Boolean": true,
  "numberAsString" : "6.0",
}
```

### Visitors

This library's classes support visitors. Visitors allow us to perform an operation on a group of objects by visiting every object in a given hierarchy.
The following methods exist in the visitor interface:
- `visit(jsonLeaf: JsonLeaf<*>)` : Visits JSON Leaf elements.
- `visit(jsonComposite: JsonComposite)` : Visits JSON Composite elements.
- `visit(name: String, jsonElement: JsonElement): Boolean` : Visits JSON Elements with knowledge of their associated property names.
- `endVisit(jsonComposite: JsonComposite)` : Used to indicate the that a visitor is ending its visit of a certain composite.

And all JSON elements have the following methods to accept visitors:
- `accept(visitor: Visitor)`
- `accept(visitor: Visitor, name: String): Boolean`

`JsonObject.getValuesOfProperty(propertyName: String): List<JsonElement>` is a method in the JSON library that obtains a list of JSON element with the given name, inside a certain jsonObject.
This method is implemented using visitors: 

```kotlin
fun JsonObject.getJsonObjectWithProperties(properties: List<String>): List<JsonObject> {
    val result = object : Visitor {
        val elementList = mutableListOf<JsonObject>()
        override fun visit(jsonComposite: JsonComposite) {
            if (jsonComposite is JsonObject)
                if(jsonComposite.elements.keys.containsAll(properties))
                    elementList.add(jsonComposite)
        }
    }
    this.accept(result)
    return result.elementList
}
```

In this example we use the `visit(jsonComposite: JsonComposite)` function to visit every composite element and analyse its elements.
If, for example, you want to visit every element with reference to its name, you may use the `visit(name: String, jsonElement: JsonElement): Boolean` function.

### Observers

This library makes use of the Observer Pattern. This pattern utilizes an interface where each operation represents the reactions to observable operations. When an observable operation is executed, the model notifies all the observers by invoking an operation from the interface.

An example of an interface is the JsonEditorViewObserver. This interface has the following methods to notify the model about the changes made in the Editor UI:
- `elementAddedToObject(modelObject: JsonObject, name: String, value: JsonElement)`
- `elementAddedToArray(modelArray: JsonArray, value: JsonElement, index: Int)`
- `elementRemovedFromObject(modelObject: JsonObject, name: String)`
- `elementRemovedFromArray(modelArray: JsonArray, index: Int)`
- `elementModifiedInObject(modelObject: JsonObject, name: String, newValue: JsonLeaf<*>)`
- `elementModifiedInArray(modelArray: JsonArray, index: Int, newValue: JsonLeaf<*>)`

When a value associated with a propriety in a JsonObject is changed in the Editor View the `JsonEditorViewObserver.elementModifiedInObject()` method is called. This method is responsible for notifying the Controller, which is observing the Editor View through the `JsonEditorViewObserver` interface , about the change.

Once notified, the Controller can then notify the JsonObject model about the change made.

```kotlin
editorView.addObserver(object : JsonEditorViewObserver {
  override fun elementModifiedInObject(modelObject: JsonObject, name: String, newValue: JsonLeaf<*>) {
    val oldValue = modelObject.elements[name]
    if(oldValue is JsonLeaf<*> && oldValue.value != newValue.value) {
      val cmd = ModifyInObjectCommand(modelObject, name, oldValue, newValue)
      runCommandAndUpdateStack(cmd)
    }
  }
})
```
Where:
```kotlin
class ModifyInObjectCommand(private val model: JsonObject, private val name: String, private val oldValue: JsonLeaf<*>, private val newValue: JsonLeaf<*>): Command {
  override fun run() {
    model.modifyElement(name, newValue)
  }
  
  override fun undo() {
     model.modifyElement(name, oldValue)
  }
}
```
