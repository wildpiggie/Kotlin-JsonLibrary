import java.awt.Component
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class JsonEditorView(private val model: JsonElement) : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT

        add(createJsonElementWidget("aa", model)) // opçao sem propriedade/nomeS

        // menu
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val menu = JPopupMenu("Message")
                    val add = JButton("add")
                    add.addActionListener {
                        val text = JOptionPane.showInputDialog("text")
                        // necessario dois inputs, nome e valor
                        add(createJsonElementWidget("aaaa",model))
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
        })
    }

    fun createJsonElementWidget(key: String, value: JsonElement): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            alignmentX = Component.LEFT_ALIGNMENT
            alignmentY = Component.TOP_ALIGNMENT

            if (value is JsonComposite) {
                when(value) {
                    is JsonObject -> value.elements.forEach {
                        add(createJsonElementWidget(it.key, it.value))
                    }
                    //is JsonArray -> value.elements.forEach {}
                }
            }
            add(JLabel(key))
            val text = JTextField(value.getStructure())
            println("Criei widget")
            text.addFocusListener(object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    println("perdeu foco: ${text.text}")
                }
            })
            add(text)
        }

    // adicionar uma flag para diferenciar se a property era opcional
}

