import java.awt.Color
import java.awt.Component
import javax.swing.*

class JsonEditorView(model: JsonObject) : JPanel() {
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
            alignmentX = Component.LEFT_ALIGNMENT
            alignmentY = Component.TOP_ALIGNMENT

            add(JTextField(value.toString()))
        }
    }
    inner class JsonObjectWidget(private val value: JsonObject): JsonWidget() {
        val widgets = mutableListOf<JsonWidget>()
        init {
            border = BorderFactory.createLineBorder(Color.BLACK)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            value.addObserver(object : JsonObjectObserver {
                override fun elementAdded(name: String, value: JsonElement) {
                    val panel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        add(JLabel(name))
                        when(value) {
                            is JsonLeaf<*> -> {
                                var widget = JsonLeafWidget(value)
                                add(widget)
                                widgets.add(widget)
                            }
                            is JsonObject -> {
                                var widget = JsonObjectWidget(value)
                                add(widget)
                                widgets.add(widget)
                            }
                        }
                        revalidate()
                        repaint()
                    }
                    add(panel)
                }
            })

            // observer mouse click
        }

        /** fun addWidgetElement(value: JsonElement) {
            when(value) {
                is JsonLeaf<*> -> {
                    add(JsonLeafWidget(value))
                }
                is JsonObject -> add(JsonObjectWidget(value))
            }
            revalidate()
            repaint()
        }
        **/
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

