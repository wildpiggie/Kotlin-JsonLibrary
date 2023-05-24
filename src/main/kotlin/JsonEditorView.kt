import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

class JsonEditorView(model: JsonObject, val jsonTextView: JsonTextView) : JPanel() {

    // no futuro adicionar a lista de observers e a interface jsoneditorview observer
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT
        /**
        addMouseListener(object : MouseAdapter() {

            /** Comandos para criar/apagar elementos **/


             * Isto vai para os observers em cada class dos widgets
             * override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val menu = JPopupMenu("Message")
                    val add = JButton("add")
                    add.addActionListener {
                        val text = JOptionPane.showInputDialog("text")
                        // necessario dois inputs, nome e valor
                        //add(createJsonElementWidget(text, value))
                        menu.isVisible = false
                        revalidate()
                        repaint()
                    }
                    val del = JButton("delete all")
                    del.addActionListener {
                        components.forEach {
                            remove(it)
                        }
                        menu.isVisible = false
                        revalidate()
                        repaint()
                    }
                    menu.add(add)
                    menu.add(del)
                    menu.show(e.component, 100, 100)
                }
            }

        }) **/

        add(JsonObjectWidget(model))
    }
    inner class JsonLeafWidget(value: JsonLeaf<*>): JsonWidget() {
        init {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.RIGHT_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
            border = BorderFactory.createLineBorder(
                Color.GREEN, 2, true)
            when (value) {

                is JsonBoolean -> {
                    val checkBox = JCheckBox()
                    checkBox.isSelected = value.value
                    add(checkBox)
                }
                //se preferirmos pode ser um textfield vazio
                //mas para isso temos que verificar no observer se estiver vazio para dizer que é null
                // e não string
                is JsonNull -> {
                    add(JLabel("null"))
                }
                is JsonString -> {
                    val textField = JTextField(7)
                    textField.maximumSize = Dimension(textField.maximumSize.width, textField.preferredSize.height)
                    textField.text = value.value
                    add(textField)
                }
                else -> {
                    val textField = JTextField(7)
                    textField.maximumSize = Dimension(textField.maximumSize.width, textField.preferredSize.height)
                    textField.text = value.toString()
                    add(textField)
                }
            }
            add(Box.createHorizontalGlue())

            jsonTextView.refresh()
        }
    }
    inner class JsonObjectWidget(modelObject: JsonObject): JsonWidget() {
        val widgets = mutableMapOf<String, JsonWidget>()
        init {
            border = BorderFactory.createLineBorder(
                Color.BLACK, 2, true)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            modelObject.addObserver(object : JsonObjectObserver {
                override fun elementAdded(name: String, value: JsonElement) {
                    val panel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        alignmentX = Component.LEFT_ALIGNMENT
                        alignmentY = Component.CENTER_ALIGNMENT

                        add(Box.createHorizontalStrut(10))
                        val label = JLabel(name)
                        add(label)
                        add(Box.createHorizontalStrut(10))

                        var widget: JsonWidget = JsonLeafWidget(JsonNull())
                        when(value) {
                            is JsonLeaf<*> -> {
                                widget = JsonLeafWidget(value)
                            }
                            is JsonObject -> {
                                widget = JsonObjectWidget(value)
                            }
                        }
                        add(widget)
                        widgets[name] = widget

                        add(Box.createHorizontalStrut(10))
                        revalidate()
                        repaint()
                    }
                    add(panel)
                }
            })
            // observer mouse click

            jsonTextView.refresh()
        }
    }

    /** inner class JsonArrayWidget(name: String, value: JsonArray): JPanel() {
        /**
         * No JsonArray os elementos nao tem nomes, como fazer para lidar com isso
         */
        fun addWidgetElement(name: String, value: JsonElement) {
            when(value) {
                is JsonLeaf<*> -> add(JsonLeafWidget(name, value))
                is JsonObject -> add(JsonObjectWidget(name, value))
                is JsonArray -> add(JsonArrayWidget(name, value))
            }
        }
    }
    **/

    abstract class JsonWidget : JPanel()

}

