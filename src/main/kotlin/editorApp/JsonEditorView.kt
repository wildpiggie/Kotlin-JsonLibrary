package editorApp

import jsonLibrary.*
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

object StringConstants {
    const val LEAF_ADD = "Add Leaf"
    const val LEAF_PROMPT = "New Leaf Name:"
    const val OBJECT_ADD = "Add Object"
    const val OBJECT_PROMPT = "New Object Name:"
    const val OBJECT_MENU = "Object Menu"
    const val ARRAY_ADD = "Add Array"
    const val ARRAY_PROMPT = "New Array Name:"
    const val REMOVE = "Remove Element"
}

class JsonEditorView(model: JsonObject) : JPanel() {

    private val observers: MutableList<JsonEditorViewObserver> = mutableListOf()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT

        add(JsonObjectWidget(model))
    }

    fun addObserver(observer: JsonEditorViewObserver) {
        observers.add(observer)
    }

    inner class JsonLeafWidget(value: JsonLeaf<*>): JsonWidget() {
        var textField = JTextField()
        var checkBox = JCheckBox()

        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.RIGHT_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
            when (value) {
                is JsonBoolean -> {
                    checkBox = JCheckBox()
                    checkBox.isSelected = value.value
                    add(checkBox)
                }
                is JsonNull -> {
                    add(JLabel("null"))
                }
                is JsonString -> {
                    textField = JTextField(7)
                    textField.maximumSize = Dimension(textField.maximumSize.width, textField.preferredSize.height)
                    textField.text = value.value
                    add(textField)
                }
                else -> {
                    textField = JTextField(7)
                    textField.maximumSize = Dimension(textField.maximumSize.width, textField.preferredSize.height)
                    textField.text = value.toString()
                    add(textField)
                }
            }
        }
    }

    inner class JsonObjectWidget(private val modelObject: JsonObject) : JsonWidget() {
        init {

            modelObject.elements.forEach{
                add(ObjectElementWidget(it.key, it.value))
            }

            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createLineBorder(Color.BLACK, 5)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    grabFocus()
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val menu = JPopupMenu()
                        val buttonAddLeaf = JMenuItem(StringConstants.LEAF_ADD)
                        val buttonAddObject = JMenuItem(StringConstants.OBJECT_ADD)
                        val buttonAddArray = JMenuItem(StringConstants.ARRAY_ADD)

                        buttonAddLeaf.addActionListener {
                            val leafName = JOptionPane.showInputDialog(null, StringConstants.LEAF_PROMPT,
                                StringConstants.OBJECT_MENU, JOptionPane.QUESTION_MESSAGE)
                            if(!leafName.isNullOrEmpty())
                                leafName.let {
                                    observers.forEach {
                                        it.elementAddedToObject(modelObject, leafName, JsonNull())
                                    }
                                }
                            menu.isVisible = false
                        }

                        buttonAddObject.addActionListener {
                            val objectName = JOptionPane.showInputDialog(null, StringConstants.OBJECT_PROMPT,
                                StringConstants.OBJECT_MENU, JOptionPane.QUESTION_MESSAGE)
                            if(!objectName.isNullOrEmpty())
                                objectName.let {
                                    observers.forEach {
                                        it.elementAddedToObject(modelObject, objectName, JsonObject())
                                    }
                                }
                            menu.isVisible = false
                        }

                        buttonAddArray.addActionListener {
                            val arrayName = JOptionPane.showInputDialog(null, StringConstants.ARRAY_PROMPT,
                                StringConstants.OBJECT_MENU, JOptionPane.QUESTION_MESSAGE)
                            if(!arrayName.isNullOrEmpty())
                                arrayName.let {
                                    observers.forEach {
                                        it.elementAddedToObject(modelObject, arrayName, JsonArray())
                                    }
                                }
                            menu.isVisible = false
                        }

                        menu.add(buttonAddLeaf)
                        menu.add(buttonAddObject)
                        menu.add(buttonAddArray)
                        menu.show(e.component, e.x, e.y)
                    }
                }
            })

            modelObject.addObserver(object : JsonObjectObserver {
                override fun elementAdded(name: String, value: JsonElement, index: Int) {
                    add(ObjectElementWidget(name, value), index)
                    revalidate()
                    repaint()
                }

                override fun elementModified(name: String, newValue: JsonElement, index: Int) {
                    remove(this@JsonObjectWidget.getComponent(index))
                    add(ObjectElementWidget(name, newValue), index)
                    revalidate()
                    repaint()
                }

                override fun elementRemoved(name: String, index: Int) {
                    remove(this@JsonObjectWidget.getComponent(index))
                    revalidate()
                    repaint()
                }
            })

        }

        inner class ObjectElementWidget(name: String, value: JsonElement) : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                alignmentY = Component.CENTER_ALIGNMENT

                val objectElement = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    alignmentX = Component.LEFT_ALIGNMENT
                    alignmentY = Component.CENTER_ALIGNMENT

                    add(Box.createHorizontalStrut(10))
                    val label = JLabel(name)
                    add(label)
                    add(Box.createHorizontalStrut(10))

                    label.addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            grabFocus()
                            if (SwingUtilities.isRightMouseButton(e)) {
                                val menu = JPopupMenu()
                                val removeButton = JMenuItem(StringConstants.REMOVE)

                                removeButton.addActionListener {
                                    observers.forEach {
                                        it.elementRemovedFromObject(modelObject, name)
                                    }
                                    menu.isVisible = false
                                }
                                menu.add(removeButton)
                                menu.show(e.component, e.x, e.y)
                            }
                        }
                    })

                    var jsonElementWidget: JsonWidget = JsonLeafWidget(JsonNull())
                    when(value) {
                        is JsonLeaf<*> -> {
                            jsonElementWidget = JsonLeafWidget(value)

                            when (value) {
                                is JsonNull ->
                                    jsonElementWidget.addMouseListener(object : MouseAdapter() {
                                        override fun mouseClicked(e: MouseEvent) {
                                            grabFocus()
                                            if (SwingUtilities.isLeftMouseButton(e)) {
                                                observers.forEach {
                                                    it.elementModifiedInObject(modelObject,
                                                        name,
                                                        JsonString(""),)
                                                }
                                            }
                                        }
                                    })

                                is JsonBoolean ->
                                    jsonElementWidget.checkBox.addFocusListener(object : FocusAdapter() {
                                        override fun focusLost(e: FocusEvent?) {
                                            observers.forEach {
                                                it.elementModifiedInObject(modelObject,
                                                    name,
                                                    JsonBoolean(((jsonElementWidget as JsonLeafWidget).checkBox.isSelected)))
                                            }
                                        }
                                    })

                                else ->
                                    jsonElementWidget.textField.addFocusListener(object : FocusAdapter() {
                                        override fun focusLost(e: FocusEvent) {
                                            observers.forEach {
                                                it.elementModifiedInObject(modelObject, name,
                                                    getModifiedLeafType((jsonElementWidget as JsonLeafWidget).textField.text))
                                            }
                                        }
                                    })
                            }
                        }

                        is JsonObject -> {
                            jsonElementWidget = JsonObjectWidget(value)
                        }
                        is JsonArray -> {
                            jsonElementWidget = JsonArrayWidget(value)
                        }
                    }
                    add(jsonElementWidget)

                    add(Box.createHorizontalStrut(10))
                    revalidate()
                    repaint()
                }
                add(objectElement)
            }
        }
    }

    inner class JsonArrayWidget(private val modelArray: JsonArray): JsonWidget() {
        init {
            modelArray.elements.forEach {
                add(ArrayElementWidget(it))
            }

            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createLineBorder(Color.DARK_GRAY, 5)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    onMouseClicked(e)
                }
            })

            modelArray.addObserver(object : JsonArrayObserver {
                override fun elementAdded(value: JsonElement) {
                    add(ArrayElementWidget(value))
                    updateBorders()
                    revalidate()
                    repaint()
                }

                override fun elementAdded(value: JsonElement, index: Int) {
                    add(ArrayElementWidget(value), index)
                    updateBorders()
                    revalidate()
                    repaint()
                }

                override fun elementModified(index: Int, newValue: JsonElement) {
                    remove(this@JsonArrayWidget.getComponent(index))
                    add(ArrayElementWidget(newValue), index)
                    updateBorders()
                    revalidate()
                    repaint()
                }

                override fun elementRemoved(index: Int) {
                    remove(this@JsonArrayWidget.getComponent(index))
                    updateBorders()
                    revalidate()
                    repaint()
                }
            })

            updateBorders()
        }

        private fun updateBorders() {
            this.components.forEach {
                if (it is ArrayElementWidget) {
                    val borderComponent = BorderFactory.createLineBorder(
                        if (this.components.indexOf(it) % 2 == 0) Color(220,220,220) else Color(190,190,190), 10
                    )
                    it.border = borderComponent
                }
            }
        }

        private fun onMouseClicked(e: MouseEvent, widget: ArrayElementWidget? = null){
            grabFocus()
            var index = 0

            if(widget != null)
                index = this@JsonArrayWidget.components.indexOf(widget) + 1

            if (SwingUtilities.isRightMouseButton(e)) {
                val menu = JPopupMenu()
                val buttonAddLeaf = JMenuItem(StringConstants.LEAF_ADD)
                val buttonAddObject = JMenuItem(StringConstants.OBJECT_ADD)
                val buttonAddArray = JMenuItem(StringConstants.ARRAY_ADD)

                buttonAddLeaf.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, index, JsonNull())
                    }
                    menu.isVisible = false
                }

                buttonAddObject.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, index, JsonObject())
                    }
                    menu.isVisible = false
                }

                buttonAddArray.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, index, JsonArray())
                    }
                    menu.isVisible = false
                }

                menu.add(buttonAddLeaf)
                menu.add(buttonAddObject)
                menu.add(buttonAddArray)

                if(widget!=null){
                    val remove = JMenuItem(StringConstants.REMOVE)
                    remove.addActionListener {
                        observers.forEach {
                            it.elementRemovedFromArray(modelArray, index - 1)
                        }
                        menu.isVisible = false
                    }
                    menu.add(remove)
                }
                menu.show(e.component, e.x, e.y)
            }
        }

        inner class ArrayElementWidget(value: JsonElement) : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                alignmentY = Component.CENTER_ALIGNMENT

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        onMouseClicked(e, this@ArrayElementWidget)
                    }
                })

                var widget: JsonWidget = JsonLeafWidget(JsonNull())
                when (value) {
                    is JsonLeaf<*> -> {
                        widget = JsonLeafWidget(value)

                        when (value) {
                            is JsonNull ->
                                widget.addMouseListener(object : MouseAdapter() {
                                    override fun mouseClicked(e: MouseEvent) {
                                        grabFocus()
                                        if (SwingUtilities.isLeftMouseButton(e)) {
                                            observers.forEach {
                                                it.elementModifiedInArray(modelArray,
                                                    this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                                    JsonString("")
                                                )
                                            }
                                        }
                                    }
                                })

                            is JsonBoolean ->
                                widget.checkBox.addFocusListener(object : FocusAdapter() {
                                    override fun focusLost(e: FocusEvent?) {
                                        observers.forEach {
                                            it.elementModifiedInArray(modelArray,
                                                this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                                JsonBoolean((widget as JsonLeafWidget).checkBox.isSelected))
                                        }
                                    }
                                })

                            else ->
                                widget.textField.addFocusListener(object : FocusAdapter() {
                                    override fun focusLost(e: FocusEvent) {
                                        observers.forEach {
                                            it.elementModifiedInArray(modelArray,
                                                this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                                getModifiedLeafType((widget as JsonLeafWidget).textField.text))
                                        }
                                    }
                                })
                        }
                    }

                    is JsonObject -> {
                        widget = JsonObjectWidget(value)
                    }

                    is JsonArray -> {
                        widget = JsonArrayWidget(value)
                    }
                }
                add(widget)
                revalidate()
                repaint()
            }
        }
    }

    fun getModifiedLeafType(value: String): JsonLeaf<*> =
        when {
            value == "true" -> JsonBoolean(true)
            value == "false" -> JsonBoolean(false)
            value.isEmpty() -> JsonNull()
            value.toIntOrNull() != null -> JsonNumber(value.toInt())
            value.toDoubleOrNull() != null -> JsonNumber(value.toDouble())
            else -> JsonString(value)
        }
}
abstract class JsonWidget : JPanel()

interface JsonEditorViewObserver {
    fun elementAddedToObject(modelObject: JsonObject, name: String, value: JsonElement)
    fun elementAddedToArray(modelArray: JsonArray, index: Int, value: JsonElement)

    fun elementRemovedFromObject(modelObject: JsonObject, name: String)
    fun elementRemovedFromArray(modelArray: JsonArray, index: Int)

    fun elementModifiedInObject(modelObject: JsonObject, name: String, newValue: JsonLeaf<*>)
    fun elementModifiedInArray(modelArray: JsonArray, index: Int, newValue: JsonLeaf<*>)
}