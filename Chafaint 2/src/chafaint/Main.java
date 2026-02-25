/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package chafaint;

import java.util.ArrayList; 
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

/**
 *
 * @author rescamilla
 */

public class Main extends javax.swing.JFrame {
    // --- VARIABLES GLOBALES ---
    private Color currentColor = Color.BLACK;
    private int currentSize = 10;
    private static final String APP_VERSION = "0.2.3";
    private static final String APP_AUTHOR = "rescamilla"; 
    private static final String GIT_AUTHOR = "https://github.com/RichyKunBv";
    private static final String APP_GIT = "https://github.com/RichyKunBv/Chafaint-2";
    private LienzoPanel lienzo;

    
    // Variables para feedback temporal
private boolean mostrandoPuntoTemporal = false;
private int tempPuntoX, tempPuntoY;          // para un punto genérico
// Para círculo (ya tenemos tempCentroX, tempCentroY)
private boolean mostrandoRadio = false;
private int tempRadioX, tempRadioY;          // segundo punto del círculo
// Para elipse (ya tenemos tempFoco1, tempFoco2)
private boolean mostrandoSegundoFoco = false;
private boolean mostrandoTercerPunto = false;
    
    // Variables manuales (Por seguridad)
    private javax.swing.JLabel jTxtPositionX;
    private javax.swing.JLabel jTxtPositionY;
    private javax.swing.JLabel jTxtColorCode;
    
    // --- NUEVAS VARIABLES PARA CÍRCULOS Y ELIPSES ---
    private ArrayList<Circulo> listaCirculos = new ArrayList<>();
    private ArrayList<Elipse> listaElipses = new ArrayList<>();
    
    private enum ModoDibujo { POLILINEA, CIRCULO, ELIPSE }
    private ModoDibujo modoActual = ModoDibujo.POLILINEA;
    
    // Variables auxiliares para captura de puntos
    private int pasoCirculo = 0;          // 0: esperando centro, 1: esperando radio
    private int pasoElipse = 0;           // 0: foco1, 1: foco2, 2: punto elipse
    private int tempCentroX, tempCentroY; // centro temporal
    private int tempFoco1X, tempFoco1Y, tempFoco2X, tempFoco2Y; // focos temporales
    
    // Constante para la tangente (3 cm ≈ 113 píxeles a 96dpi)
    private static final int TANGENT_LENGTH = 113;
    
    // Componentes de UI nuevos
    private JRadioButton jRadioPolilinea;
    private JRadioButton jRadioCirculo;
    private JRadioButton jRadioElipse;
    private ButtonGroup buttonGroupModos;
    private JLabel jLabelEstadoModo;
    private JSlider jSliderLados;      // slider para número de lados
    private JLabel jLabelLados;        // muestra el valor actual
        
    
    class LienzoPanel extends JPanel {
        public LienzoPanel() {
            this.setBackground(Color.WHITE);
        }
        
        @Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g); 
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

    // --- Dibujar círculos (polígonos regulares con tangentes) ---
    for (Circulo c : listaCirculos) {
        g2.setColor(c.color);
        g2.setStroke(new BasicStroke(c.grosor));
        
        int n = c.numLados;
        double[] xPoints = new double[n];
        double[] yPoints = new double[n];
        double angleStep = 2 * Math.PI / n;
        
        for (int i = 0; i < n; i++) {
            double angle = i * angleStep;
            xPoints[i] = c.centroX + c.radio * Math.cos(angle);
            yPoints[i] = c.centroY + c.radio * Math.sin(angle);
        }
        
        // Dibujar el polígono
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            g2.drawLine((int)xPoints[i], (int)yPoints[i], (int)xPoints[j], (int)yPoints[j]);
        }
        
        // Dibujar tangentes de 3 cm en cada vértice
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.GRAY);
        for (int i = 0; i < n; i++) {
            double angle = i * angleStep;
            double tx = -Math.sin(angle);
            double ty = Math.cos(angle);
            double half = TANGENT_LENGTH / 2.0;
            int x1 = (int)(xPoints[i] - tx * half);
            int y1 = (int)(yPoints[i] - ty * half);
            int x2 = (int)(xPoints[i] + tx * half);
            int y2 = (int)(yPoints[i] + ty * half);
            g2.drawLine(x1, y1, x2, y2);
        }
    }
    
    // --- Dibujar elipses (definidas por focos) ---
    for (Elipse e : listaElipses) {
        g2.setColor(e.color);
        g2.setStroke(new BasicStroke(e.grosor));
        
        int puntos = 100; // resolución
        double[] xP = new double[puntos + 1];
        double[] yP = new double[puntos + 1];
        
        for (int i = 0; i <= puntos; i++) {
            double t = 2 * Math.PI * i / puntos;
            double xLocal = e.a * Math.cos(t);
            double yLocal = e.b * Math.sin(t);
            
            // Rotar según el ángulo de los focos
            double xRot = xLocal * Math.cos(e.angulo) - yLocal * Math.sin(e.angulo);
            double yRot = xLocal * Math.sin(e.angulo) + yLocal * Math.cos(e.angulo);
            
            xP[i] = e.centroX + xRot;
            yP[i] = e.centroY + yRot;
        }
        
        for (int i = 0; i < puntos; i++) {
            g2.drawLine((int)xP[i], (int)yP[i], (int)xP[i+1], (int)yP[i+1]);
        }
        
        // Marcar focos y punto de definición (se mantienen siempre visibles)
        g2.setColor(Color.RED);
        g2.fillOval(e.foco1X - 3, e.foco1Y - 3, 6, 6);
        g2.fillOval(e.foco2X - 3, e.foco2Y - 3, 6, 6);
        g2.setColor(Color.BLUE);
        g2.fillOval(e.puntoX - 3, e.puntoY - 3, 6, 6);
    }

    g2.setStroke(new BasicStroke(1));
    g2.setColor(new Color(0, 150, 255, 200)); 

    if (mostrandoPuntoTemporal) {
        g2.fillOval(tempPuntoX - 5, tempPuntoY - 5, 10, 10);
    }

    if (mostrandoRadio) {
        g2.fillOval(tempRadioX - 5, tempRadioY - 5, 10, 10);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(tempCentroX, tempCentroY, tempRadioX, tempRadioY);
    }

    if (mostrandoSegundoFoco) {
        g2.setColor(new Color(0, 150, 255, 200));
        g2.fillOval(tempFoco1X - 5, tempFoco1Y - 5, 10, 10);
        g2.fillOval(tempFoco2X - 5, tempFoco2Y - 5, 10, 10);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(tempFoco1X, tempFoco1Y, tempFoco2X, tempFoco2Y);
    }

    if (mostrandoTercerPunto) {
     
    }
}
    }

    // CLASE NODO
    class Nodo {
        int x, y; Color color; int grosor;
        public Nodo(int x, int y, Color color, int grosor) {
            this.x = x; this.y = y; this.color = color; this.grosor = grosor;
        }
    }
    
    class Circulo {
        int centroX, centroY;
        double radio;
        int numLados;        // 3 a 360
        Color color;
        int grosor;

        public Circulo(int cx, int cy, double r, int n, Color c, int g) {
            centroX = cx; centroY = cy; radio = r; numLados = n; color = c; grosor = g;
        }
    }

    class Elipse {
        int foco1X, foco1Y, foco2X, foco2Y;
        int puntoX, puntoY;   // punto sobre la elipse
        double a, b;          // semiejes mayor y menor
        double centroX, centroY;
        double angulo;        // rotación
        Color color;
        int grosor;

        public Elipse(int fx1, int fy1, int fx2, int fy2, int px, int py, Color c, int g) {
            foco1X = fx1; foco1Y = fy1;
            foco2X = fx2; foco2Y = fy2;
            puntoX = px; puntoY = py;
            color = c; grosor = g;

            // Centro
            centroX = (fx1 + fx2) / 2.0;
            centroY = (fy1 + fy2) / 2.0;

            // Semidistancia focal c
            double cDist = Math.hypot(fx2 - fx1, fy2 - fy1) / 2.0;

            // Suma de distancias constante 2a
            double suma = Math.hypot(px - fx1, py - fy1) + Math.hypot(px - fx2, py - fy2);
            double aVal = suma / 2.0;

            // Semieje menor b = sqrt(a^2 - c^2)
            double bVal = Math.sqrt(aVal * aVal - cDist * cDist);

            // Ángulo de rotación
            angulo = Math.atan2(fy2 - fy1, fx2 - fx1);

            this.a = aVal;
            this.b = bVal;
        }
    }
    
    // Logger
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Main.class.getName());
    
    /**
     * Creates new form Main
     */
public Main() {
    initComponents();

    setTitle("Chafaint 2 Premium Delux Super Papu Pro Redondo Edition");
    
    // Icono
    try {
        java.net.URL urlIcono = getClass().getResource("/chafaint/Miku.png");
        if (urlIcono == null) {
            urlIcono = getClass().getResource("Miku.png");
        }
        
        if (urlIcono != null) {
            java.awt.Image icono = new javax.swing.ImageIcon(urlIcono).getImage();
            setIconImage(icono);
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icono);
                }
            }
        } else {
            System.err.println("¡No se encontró Miku.png!");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    // Evitar que las toolbars se puedan arrastrar
    jToolBar1.setFloatable(false);
    jToolBar2.setFloatable(false);

    // Inicializar etiquetas manuales
    jLabelVersion.setText("Versión: " + APP_VERSION);    

    // --- RELOJ ---
    Timer timer = new Timer(1000, e -> {
        LocalDateTime now = LocalDateTime.now();
        if(jLabelDay != null) jLabelDay.setText("Día: " + now.toLocalDate());
        if(jLabelHour != null) jLabelHour.setText("Hora: " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    });
    timer.start();
    
    // --- LIENZO ---
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(jToolBar1, BorderLayout.NORTH);
    getContentPane().add(jToolBar2, BorderLayout.SOUTH);
    lienzo = new LienzoPanel();
    getContentPane().add(lienzo, BorderLayout.CENTER);
    
    // --- NUEVOS COMPONENTES DE INTERFAZ ---
    jRadioCirculo   = new JRadioButton("Círculo");
    jRadioElipse    = new JRadioButton("Elipse");
    buttonGroupModos = new ButtonGroup();
    buttonGroupModos.add(jRadioCirculo);
    buttonGroupModos.add(jRadioElipse);
    jRadioCirculo.setSelected(true);
    
    // Eliminamos los botones antiguos de la barra (Open, Save, etc.) y añadimos los nuevos
    // Nota: como los botones están en la barra por el diseñador, los quitamos manualmente
    jToolBar1.add(jRadioCirculo);
    jToolBar1.add(jRadioElipse);
    
    jLabelLados = new JLabel("Lados:");
    jSliderLados = new JSlider(3, 360, 360);
    jSliderLados.setMajorTickSpacing(50);
    jSliderLados.setMinorTickSpacing(10);
    jSliderLados.setPaintTicks(true);
    jSliderLados.setPaintLabels(true);
    jSliderLados.setEnabled(true); // siempre activo porque solo hay círculo
    
    jToolBar1.add(new javax.swing.JToolBar.Separator());
    jToolBar1.add(jLabelLados);
    jToolBar1.add(jSliderLados);
    
    jLabelEstadoModo = new JLabel("Modo: Círculo (clic centro)");
    jToolBar2.add(new javax.swing.JToolBar.Separator());
    jToolBar2.add(jLabelEstadoModo);
    
    // Listeners para los radio buttons
    jRadioCirculo.addActionListener(e -> {
        modoActual = ModoDibujo.CIRCULO;
        jLabelEstadoModo.setText("Modo: Círculo (clic centro)");
        reiniciarCaptura();
    });
    jRadioElipse.addActionListener(e -> {
        modoActual = ModoDibujo.ELIPSE;
        jLabelEstadoModo.setText("Modo: Elipse (clic foco 1)");
        reiniciarCaptura();
    });
    
    // Slider listener: actualiza el último círculo
    jSliderLados.addChangeListener(e -> {
        if (!listaCirculos.isEmpty() && modoActual == ModoDibujo.CIRCULO) {
            int lados = jSliderLados.getValue();
            listaCirculos.get(listaCirculos.size() - 1).numLados = lados;
            lienzo.repaint();
        }
    });
    
    // --- EVENTOS DEL RATÓN ---
    javax.swing.event.MouseInputAdapter mouseHandler = new javax.swing.event.MouseInputAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (javax.swing.SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 1) {
                switch (modoActual) {
                    case CIRCULO:
    if (pasoCirculo == 0) {
        tempCentroX = evt.getX();
        tempCentroY = evt.getY();
        mostrandoPuntoTemporal = true;   // muestra el centro
        tempPuntoX = tempCentroX;
        tempPuntoY = tempCentroY;
        pasoCirculo = 1;
        jLabelEstadoModo.setText("Círculo: clic para radio");
    } else {
        tempRadioX = evt.getX();
        tempRadioY = evt.getY();
        mostrandoRadio = true;            // muestra el punto del radio
        double r = Math.hypot(tempRadioX - tempCentroX, tempRadioY - tempCentroY);
        int lados = jSliderLados.getValue();
        listaCirculos.add(new Circulo(tempCentroX, tempCentroY, r, lados, currentColor, currentSize));
        // Limpiar temporales
        mostrandoPuntoTemporal = false;
        mostrandoRadio = false;
        pasoCirculo = 0;
        jLabelEstadoModo.setText("Modo: Círculo (clic centro)");
        lienzo.repaint();
    }
    break;

                    case ELIPSE:
    if (pasoElipse == 0) {
        tempFoco1X = evt.getX();
        tempFoco1Y = evt.getY();
        mostrandoPuntoTemporal = true;
        tempPuntoX = tempFoco1X;
        tempPuntoY = tempFoco1Y;
        pasoElipse = 1;
        jLabelEstadoModo.setText("Elipse: clic foco 2");
    } else if (pasoElipse == 1) {
        tempFoco2X = evt.getX();
        tempFoco2Y = evt.getY();
        mostrandoSegundoFoco = true;
        pasoElipse = 2;
        jLabelEstadoModo.setText("Elipse: clic punto sobre la elipse");
    } else {
        int px = evt.getX();
        int py = evt.getY();
        mostrandoTercerPunto = true;
        listaElipses.add(new Elipse(tempFoco1X, tempFoco1Y, tempFoco2X, tempFoco2Y,
                                     px, py, currentColor, currentSize));
        // Limpiar temporales
        mostrandoPuntoTemporal = false;
        mostrandoSegundoFoco = false;
        mostrandoTercerPunto = false;
        pasoElipse = 0;
        jLabelEstadoModo.setText("Modo: Elipse (clic foco 1)");
        lienzo.repaint();
    }
    break;
                }
            }
        }
    };
    
    lienzo.addMouseListener(mouseHandler);
    
    addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
            SALIR();
        }
    });
    
    setSize(800, 600);
    setLocationRelativeTo(null);
}




    // --- MÉTODOS DE LÓGICA (EL CEREBRO) ---

    private void reiniciarCaptura() {
    pasoCirculo = 0;
    pasoElipse = 0;
    mostrandoPuntoTemporal = false;
    mostrandoRadio = false;
    mostrandoSegundoFoco = false;
    mostrandoTercerPunto = false;
}

    private void limpiarLienzo() { 
        listaCirculos.clear();
        listaElipses.clear();
        reiniciarCaptura();
        lienzo.repaint();
    }
    
    private void ColorSelector() {
        Color c = javax.swing.JColorChooser.showDialog(this, "Color", currentColor);
        if(c!=null) currentColor = c;
    }
    
    private void SizeSelector() {
        String s = JOptionPane.showInputDialog(this, "Grosor (1-50):", currentSize);
        if (s != null) {
            try {
                int nuevo = Integer.parseInt(s);
                if (nuevo >= 1 && nuevo <= 50) {
                    currentSize = nuevo;
                } else {
                    JOptionPane.showMessageDialog(this, "El grosor debe estar entre 1 y 50.");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Debe ingresar un número entero.");
            }
        }
    }

    private void mostrarAyuda() {
    JOptionPane.showMessageDialog(this, 
        "Proyecto Chafaint 2 v" + APP_VERSION + "\n" +
        "Modo Círculo:\n" +
        "- 1er clic: centro\n" +
        "- 2do clic: radio (define tamaño)\n" +
        "- Se dibuja un polígono regular con tantos lados como indique el slider\n\n" +
        "Modo Elipse:\n" +
        "- 1er clic: foco 1\n" +
        "- 2do clic: foco 2\n" +
        "- 3er clic: punto sobre la elipse\n\n" +
        "Puedes cambiar el número de lados del círculo con el slider.\n" +
        "Para borrar todo: botón 'Limpiar Pantalla' o menú Opciones.");
}
    
    private void SALIR() {
        int confirmarSalida = JOptionPane.showConfirmDialog(this,
            "¿Está seguro que desea salir de la aplicación?",
            "Salir",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (confirmarSalida == JOptionPane.YES_OPTION) {
            this.dispose();
            System.exit(0);
        }
    }
    
        private void HV() {
        JOptionPane.showMessageDialog(this, 
            "Proyecto Chafaint 2 v" + APP_VERSION + "\n" +
            "Git del proyecto: " + APP_GIT + "\n" +
            "0.1: Creacion del proyecto\n" +
            "0.2: Se agregaron las funciones para hacer figuras redondas\n" +
            "0.2.1: Reestructura del codigo y optimizacion chafa\n" +
            "0.2.2: Mas bugueado que nunca, menos solucionado que siempre\n" +
            "0.2.3: Se añadio el link del repositorio del proyecto"
        );
    }
        
    private void AcercaDE() {                                        
        JOptionPane.showMessageDialog(this, 
            "Proyecto Chafaint 2 \n" +
            "Versión: " + APP_VERSION + "\n" +
            "Desarrollado por: " + APP_AUTHOR + "\n" +
            "Git del desarrollador: " + GIT_AUTHOR +"\n" +
            "Git del proyecto: " + APP_GIT + "\n");
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        jButtonColor = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        jButtonClear = new javax.swing.JButton();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        jButtonHelp = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jToolBar2 = new javax.swing.JToolBar();
        jLabelDay = new javax.swing.JLabel();
        jLabelHour = new javax.swing.JLabel();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        jLabelVersion = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuSize = new javax.swing.JMenuItem();
        jMenuColor = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuClear = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuHelp = new javax.swing.JMenuItem();
        jMenuAD = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        jMenuHV = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Chafaint 2 Premium Delux Super Papu Pro Redondo Edition");
        setCursor(new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));
        setPreferredSize(new java.awt.Dimension(600, 400));

        jToolBar1.setBackground(new java.awt.Color(153, 255, 255));
        jToolBar1.setForeground(new java.awt.Color(204, 255, 255));
        jToolBar1.setRollover(true);

        jButtonColor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/chafaint/cute-pencil-cartoon-by-Vexels (1).png"))); // NOI18N
        jButtonColor.setToolTipText("Color");
        jButtonColor.setFocusable(false);
        jButtonColor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonColor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonColor.addActionListener(this::jButtonColorActionPerformed);
        jToolBar1.add(jButtonColor);
        jToolBar1.add(jSeparator4);

        jButtonClear.setText("Limpiar Pantalla");
        jButtonClear.setFocusable(false);
        jButtonClear.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonClear.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonClear.addActionListener(this::jButtonClearActionPerformed);
        jToolBar1.add(jButtonClear);
        jToolBar1.add(jSeparator7);

        jButtonHelp.setText("?");
        jButtonHelp.setFocusable(false);
        jButtonHelp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonHelp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonHelp.addActionListener(this::jButtonHelpActionPerformed);
        jToolBar1.add(jButtonHelp);
        jToolBar1.add(jSeparator1);

        jToolBar2.setBackground(new java.awt.Color(204, 255, 255));
        jToolBar2.setForeground(new java.awt.Color(0, 255, 255));
        jToolBar2.setRollover(true);

        jLabelDay.setText("Dia: ");
        jToolBar2.add(jLabelDay);

        jLabelHour.setText("Hora:");
        jToolBar2.add(jLabelHour);
        jToolBar2.add(jSeparator10);

        jLabelVersion.setText("Version: ");
        jToolBar2.add(jLabelVersion);

        jMenu1.setText("Archivo");

        jMenuExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuExit.setText("Salir");
        jMenuExit.addActionListener(this::jMenuExitActionPerformed);
        jMenu1.add(jMenuExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Opciones");

        jMenuSize.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuSize.setText("Grosor de Linea");
        jMenuSize.addActionListener(this::jMenuSizeActionPerformed);
        jMenu2.add(jMenuSize);

        jMenuColor.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuColor.setText("Color");
        jMenuColor.addActionListener(this::jMenuColorActionPerformed);
        jMenu2.add(jMenuColor);
        jMenu2.add(jSeparator2);

        jMenuClear.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuClear.setText("Limpiar Pantalla");
        jMenuClear.addActionListener(this::jMenuClearActionPerformed);
        jMenu2.add(jMenuClear);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Ayuda");

        jMenuHelp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuHelp.setText("?");
        jMenuHelp.addActionListener(this::jMenuHelpActionPerformed);
        jMenu3.add(jMenuHelp);

        jMenuAD.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuAD.setText("Acerca De");
        jMenuAD.addActionListener(this::jMenuADActionPerformed);
        jMenu3.add(jMenuAD);
        jMenu3.add(jSeparator11);

        jMenuHV.setText("Historial de Versiones");
        jMenuHV.addActionListener(this::jMenuHVActionPerformed);
        jMenu3.add(jMenuHV);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE)
            .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 353, Short.MAX_VALUE)
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void jMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuExitActionPerformed
        SALIR();
    }//GEN-LAST:event_jMenuExitActionPerformed

    private void jMenuSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSizeActionPerformed
        SizeSelector(); 
    }//GEN-LAST:event_jMenuSizeActionPerformed

    private void jMenuColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuColorActionPerformed
        ColorSelector();
    }//GEN-LAST:event_jMenuColorActionPerformed

    private void jMenuClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuClearActionPerformed
        limpiarLienzo();
    }//GEN-LAST:event_jMenuClearActionPerformed

    private void jMenuHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuHelpActionPerformed
        mostrarAyuda();
    }//GEN-LAST:event_jMenuHelpActionPerformed

    private void jMenuADActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuADActionPerformed
        AcercaDE();
    }//GEN-LAST:event_jMenuADActionPerformed

    private void jButtonColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonColorActionPerformed
        ColorSelector();
    }//GEN-LAST:event_jButtonColorActionPerformed

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        limpiarLienzo();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private void jButtonHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonHelpActionPerformed
        mostrarAyuda();
    }//GEN-LAST:event_jButtonHelpActionPerformed

    private void jMenuHVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuHVActionPerformed
        HV();
    }//GEN-LAST:event_jMenuHVActionPerformed

    /**
     * @param args the command line arguments
     */
public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new Main().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonColor;
    private javax.swing.JButton jButtonHelp;
    private javax.swing.JLabel jLabelDay;
    private javax.swing.JLabel jLabelHour;
    private javax.swing.JLabel jLabelVersion;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenuAD;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuClear;
    private javax.swing.JMenuItem jMenuColor;
    private javax.swing.JMenuItem jMenuExit;
    private javax.swing.JMenuItem jMenuHV;
    private javax.swing.JMenuItem jMenuHelp;
    private javax.swing.JMenuItem jMenuSize;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    // End of variables declaration//GEN-END:variables

    
}
