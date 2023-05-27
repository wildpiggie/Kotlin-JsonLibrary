import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

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
            add(Box.createHorizontalGlue())

        }
    }

    inner class JsonObjectWidget(private val modelObject: JsonObject) : JsonWidget() {
        init {
            modelObject.elements.forEach{
                add(ObjectElementWidget(it.key, it.value))
            }

            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createLineBorder(Color.BLACK, 5)

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

        // temos de fazer um update border tambem para dar fixe as borders do objeto? -> ver o que o samuel fez na ultima versao

        inner class ObjectElementWidget(name: String, value: JsonElement) : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                alignmentY = Component.CENTER_ALIGNMENT

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            val menu = JPopupMenu("Object Button")
                            val buttonAddObject = JButton("Add Object")
                            val buttonAddArray = JButton("Add Array")
                            val buttonAddLeaf = JButton("Add Leaf")

                            buttonAddObject.addActionListener {
                                val objectName = JOptionPane.showInputDialog("Object name")
                                if(objectName.isNotEmpty()) {
                                    objectName.let {
                                        observers.forEach {
                                            it.elementAddedToObject(modelObject, objectName, JsonObject())
                                        }
                                    }
                                }
                                menu.isVisible = false
                            }

                            buttonAddArray.addActionListener {
                                val arrayName = JOptionPane.showInputDialog("Array name")
                                if(arrayName.isNotEmpty()) {
                                    arrayName.let {
                                        observers.forEach {
                                            it.elementAddedToObject(modelObject, arrayName, JsonArray())
                                        }
                                    }
                                    menu.isVisible = false
                                }
                            }

                            buttonAddLeaf.addActionListener {
                                val leafName = JOptionPane.showInputDialog("Leaf name")
                                if(leafName.isNotEmpty()) {
                                    leafName.let {
                                        observers.forEach {
                                            it.elementAddedToObject(modelObject, leafName, JsonNull())
                                        }
                                    }
                                    menu.isVisible = false
                                }
                            }

                            menu.add(buttonAddObject)
                            menu.add(buttonAddArray)
                            menu.add(buttonAddLeaf)
                            menu.show(e.component, e.x, e.y)
                        }
                    }
                })

                val panel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    alignmentX = Component.LEFT_ALIGNMENT
                    alignmentY = Component.CENTER_ALIGNMENT

                    add(Box.createHorizontalStrut(10))
                    val label = JLabel(name)
                    add(label)
                    add(Box.createHorizontalStrut(10))

                    label.addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                val menu = JPopupMenu("Element")
                                val removeButton = JButton("Remove Element")
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

                    var widget: JsonWidget = JsonLeafWidget(JsonNull())
                    when(value) {
                        is JsonLeaf<*> -> {
                            widget = JsonLeafWidget(value)
                            widget.addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: MouseEvent) {
                                    if (SwingUtilities.isLeftMouseButton(e)) {
                                        observers.forEach {
                                            if (value is JsonNull) it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonString(""),)
                                        }
                                    }
                                }
                            })

                            widget.textField.addFocusListener(object : FocusAdapter() {
                                override fun focusLost(e: FocusEvent) {
                                    observers.forEach {
                                        if ((widget as JsonLeafWidget).textField.text.toIntOrNull() != null) {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonNumber((widget as JsonLeafWidget).textField.text.toInt())
                                            )
                                        } else if ((widget as JsonLeafWidget).textField.text.toDoubleOrNull() != null) {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonNumber((widget as JsonLeafWidget).textField.text.toDouble())
                                            )
                                        } else if ((widget as JsonLeafWidget).textField.text == "true") {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonBoolean(true)
                                            )
                                        } else if ((widget as JsonLeafWidget).textField.text == "false") {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonBoolean(false)
                                            )
                                        } else if ((widget as JsonLeafWidget).textField.text.isEmpty()) {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonNull()
                                            )
                                        } else if ((widget as JsonLeafWidget).textField.text is String) {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonString((widget as JsonLeafWidget).textField.text)
                                            )
                                        } else {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonString((widget as JsonLeafWidget).textField.text)
                                            )
                                        }

                                    }
                                }
                            })

                            widget.checkBox.addFocusListener(object : FocusAdapter() {
                                override fun focusLost(e: FocusEvent?) {
                                    observers.forEach {
                                        if ((widget as JsonLeafWidget).checkBox.isSelected) {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonBoolean(true)
                                            )
                                        } else if (!((widget as JsonLeafWidget).checkBox.isSelected)) {
                                            it.elementModifiedInObject(
                                                modelObject,
                                                name,
                                                JsonBoolean(false)
                                            )
                                        }
                                    }
                                }
                            })
                        }

                        is JsonObject -> {
                            widget = JsonObjectWidget(value)
                        }
                        is JsonArray -> {
                            widget = JsonArrayWidget(value)
                        }
                    }
                    add(widget)

                    add(Box.createHorizontalStrut(10))
                    revalidate()
                    repaint()
                }

                add(panel)

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
            var index = 0

            if(widget != null)
                index = this@JsonArrayWidget.components.indexOf(widget) + 1

            if (SwingUtilities.isRightMouseButton(e)) {
                val menu = JPopupMenu("Array Button")
                val buttonAddObject = JButton("Add Object")
                val buttonAddArray = JButton("Add Array")
                val buttonAddLeaf = JButton("Add Leaf")

                buttonAddObject.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, JsonObject(), index)
                    }
                    menu.isVisible = false
                }

                buttonAddArray.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, JsonArray(), index)
                    }
                    menu.isVisible = false
                }

                buttonAddLeaf.addActionListener {
                    observers.forEach {
                        it.elementAddedToArray(modelArray, JsonNull(), index)
                    }
                    menu.isVisible = false
                }

                menu.add(buttonAddObject)
                menu.add(buttonAddArray)
                menu.add(buttonAddLeaf)

                if(widget!=null){
                    val remove = JButton("remove from array")
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

                        widget.addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                if (SwingUtilities.isLeftMouseButton(e)) {
                                    observers.forEach {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonString("")
                                        )
                                    }
                                }
                            }
                        })

                        widget.textField.addFocusListener(object : FocusAdapter() {
                            override fun focusLost(e: FocusEvent) {
                                observers.forEach {
                                    if((widget as JsonLeafWidget).textField.text.toIntOrNull() != null) {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonNumber((widget as JsonLeafWidget).textField.text.toInt())
                                        )
                                    }
                                    else if((widget as JsonLeafWidget).textField.text.toDoubleOrNull() != null) {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonNumber((widget as JsonLeafWidget).textField.text.toDouble())
                                        )
                                    }
                                    else if( (widget as JsonLeafWidget).textField.text == "true") {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonBoolean(true)
                                        )
                                    }
                                    else if( (widget as JsonLeafWidget).textField.text == "false") {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonBoolean(false)
                                        )
                                    }
                                    else if((widget as JsonLeafWidget).textField.text.isEmpty()) {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonNull()
                                        )
                                    }
                                    else if((widget as JsonLeafWidget).textField.text is String) {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonString((widget as JsonLeafWidget).textField.text)
                                        )
                                    }
                                    else if((widget as JsonLeafWidget).checkBox.isSelected) {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonBoolean(true)
                                        )
                                    }
                                    else if(!((widget as JsonLeafWidget).checkBox.isSelected)) {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonBoolean(false)
                                        )
                                    }
                                    else {
                                        it.elementModifiedInArray(
                                            modelArray,
                                            this@JsonArrayWidget.components.indexOf(this@ArrayElementWidget),
                                            JsonString((widget as JsonLeafWidget).textField.text)
                                        )
                                    }
                                }
                            }
                        })
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
}
abstract class JsonWidget : JPanel()

interface JsonEditorViewObserver {
    fun elementAddedToObject(modelObject: JsonObject, name: String, value: JsonElement)
    fun elementAddedToArray(modelArray: JsonArray, value: JsonElement, index: Int)

    fun elementRemovedFromObject(modelObject: JsonObject, name: String)
    fun elementRemovedFromArray(modelArray: JsonArray, index: Int)

    fun elementModifiedInObject(modelObject: JsonObject, name: String, newValue: JsonLeaf<*>)
    fun elementModifiedInArray(modelArray: JsonArray, index: Int, newValue: JsonLeaf<*>)
}

