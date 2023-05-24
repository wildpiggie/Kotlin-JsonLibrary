import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class JsonEditorView(model: JsonObject, val jsonTextView: JsonTextView) : JPanel() {
    private val observers: MutableList<JsonEditorViewObserver> = mutableListOf()

    // no futuro adicionar a lista de observers e a interface jsoneditorview observer
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

             // refresecar atraves do controller
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
                            is JsonArray -> {
                                widget = JsonArrayWidget(value)
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

                // elementModified()
                // elementRemoved()
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val menu = JPopupMenu("Object Button")
                        val add = JButton("add")
                        add.addActionListener {
                            //val newPair = dualPrompt("new values", "first", "second", pair.first.toString(), pair.second.toString())
                            val newLeaf = JOptionPane.showInputDialog("text")
                            newLeaf?.let {
                                observers.forEach {
                                    //it.widgetAdded(this@JsonObjectWidget)
                                    it.elementAddedToObject(modelObject, "teste", JsonString(newLeaf))
                                }
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }
                        val del = JButton("delete all")
                        del.addActionListener {
                            println("del no objeto")
                            // components.forEach { remove(it) }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }
                        menu.add(add);
                        menu.add(del)
                        menu.show(e.component, 100, 100);
                    }
                }
            })

           // jsonTextView.refresh()
        }

    }
    inner class JsonArrayWidget(modelArray: JsonArray): JsonWidget() {
        val widgets = mutableListOf<JsonWidget>()
        init {
            border = BorderFactory.createLineBorder(
                Color.BLUE, 2, true)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            modelArray.addObserver(object : JsonArrayObserver {
                override fun elementAdded(value: JsonElement) {
                    val panel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        alignmentX = Component.LEFT_ALIGNMENT
                        alignmentY = Component.CENTER_ALIGNMENT

                        add(Box.createHorizontalStrut(10))
                        add(Box.createHorizontalStrut(10))

                        var widget: JsonWidget = JsonLeafWidget(JsonNull())
                        when(value) {
                            is JsonLeaf<*> -> {
                                widget = JsonLeafWidget(value)
                            }
                            is JsonObject -> {
                                widget = JsonObjectWidget(value)
                            }
                            is JsonArray -> {
                                widget = JsonArrayWidget(value)
                            }
                        }
                        add(widget)
                        widgets.add(widget) // nao vamos saber distinguir caso haja dois elementos com o mesmo valor

                        add(Box.createHorizontalStrut(10))
                        revalidate()
                        repaint()
                    }
                    add(panel)
                }
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val menu = JPopupMenu("Array Button")
                        val add = JButton("add")
                        add.addActionListener {
                            println("array")
                            // val newPair = dualPrompt("new values", "first", "second", pair.first.toString(), pair.second.toString())
                            val newLeaf = JOptionPane.showInputDialog("text")
                            newLeaf?.let {
                                observers.forEach {
                                    //it.widgetAdded((JsonString(newLeaf)))
                                    it.elementAddedToArray(modelArray, JsonString(newLeaf))
                                }
                            }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }
                        val del = JButton("delete all")
                        del.addActionListener {
                            println("del no array")
                            //components.forEach {remove(it) }
                            menu.isVisible = false
                            revalidate()
                            repaint()
                        }
                        menu.add(add);
                        menu.add(del)
                        menu.show(e.component, 100, 100);
                    }
                }
            })

            //jsonTextView.refresh()
        }
    }

    /**
     * Talvez para facilitar ter butoes que distinguem o tipo de coisa que vamos criar
     * e.g. quando clicamos num objeto temos a opção de adicionar um objeto, um array, ou uma leaf com propriedade associada
     * no exemplo do professor o professor clicka num objeto que esta dentro do array, mas cria um null no array
     * e quando escreve "numero :" é considerado logo um objeto
     * Acho que isso envolve demasiadas verificaçoes e com diferentes butoes seria mais facil IMO
     *
     *
     *
     * Quando adicionamos ao algo via editor, fazemos como o exemplo da aula? Onde vamos ter um observer do editor no controller e quando é notificado que algo foi atualizado ele atualiza o model no controller?
     * Se sim, como distinguimos onde é que ele esta a adicionar? Agora ele assume sempre que esta so a adicionar uma leaf ao model base
     *
     * Seria errado atualizar adicionar o widget no observer do mouseclick e depois ter um observador para atualizar o modelo?
     */



}
abstract class JsonWidget : JPanel()

interface JsonEditorViewObserver {
    fun elementAddedToObject(model0bject: JsonObject, name: String, value: JsonElement)
    fun elementAddedToArray(modelArray: JsonArray, value: JsonElement)

    //fun widgetModified() {}
    //fun widgetRemoved() {}
}

