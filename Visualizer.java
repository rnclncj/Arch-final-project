// https://zetcode.com/gfx/java2d/

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

class Element {
    private String name;
    private String type;
    private String operation;
    private ArrayList<String> operands;
    private int colNum;
    private int yCoord;

    public Element(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        type = tokenizer.nextToken();
        name = tokenizer.nextToken();
        operation = tokenizer.nextToken();
        operands = new ArrayList<>();
        colNum = 0;
        yCoord = 0;
        while (tokenizer.hasMoreTokens()) {
            operands.add(tokenizer.nextToken());
        }
    }

    public String toString() {
        String str = type + " " + name + " " + operation;
        for (String operand : operands) {
            str += " " + operand;
        }
        return str;
    }

    public int getMaxColOfInputs(HashMap<String, Element> elementMap){
        int max = 0;
        for(String input : operands){
            if(!elementMap.containsKey(input))
                continue;
            int currCol = elementMap.get(input).getColNum();
            max = Math.max(currCol, max);
        }
        return max;
    }

    public int getXCoord() {
        return Visualizer.HORIZ_DIST + getColNum() * (Visualizer.WIDTH + Visualizer.HORIZ_DIST);
    }

    //returns the height of the element
    public int getHeight(){
        return Visualizer.BASE_HEIGHT * getOperands().size();
    }

    public int getOutX() {
        return getXCoord() + Visualizer.WIDTH;
    }

    public int getOutY() {
        return yCoord + getHeight() / 2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation){
        this.operation = operation;
    }
    
    public ArrayList<String> getOperands() {
        return operands;
    }

    public void setOutputPoint(int colNum, int yCoord){
        this.colNum = colNum;
        this.yCoord = yCoord;
    }

    public int getColNum(){
        return colNum;
    }

    public void setColNum(int colNum){
        this.colNum = colNum;
    }

    public int getYCoord() {
        return yCoord;
    }
}

class Panel extends JPanel {
    private HashMap<String, Element> elementMap;

    public Panel(HashMap<String, Element> em) {
        elementMap = em;
    }
    
    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        for (Element element : elementMap.values()) {
            g2d.drawRect(element.getXCoord(), element.getYCoord(), Visualizer.WIDTH, element.getHeight());
            // add input wires
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}

public class Visualizer extends JFrame {
    public static final int BASE_HEIGHT = 30;
    public static final int WIDTH = 50;
    public static final int VERT_DIST = 30;
    public static final int HORIZ_DIST = 30;

    public Visualizer(HashMap<String, Element> em) {
        initUI(em);
    }

    private void initUI(HashMap<String, Element> em) {
        Panel panel = new Panel(em);
        panel.setPreferredSize(new Dimension(1000, 1000));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize(300, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("example.txt"));
        ArrayList<Element> elementList = new ArrayList<>();
        HashMap<String, Element> elementMap = new HashMap<>();
        
        String line = "";
        while ((line = reader.readLine()) != null) {
            Element element = new Element(line);
            elementList.add(element);
            elementMap.put(element.getName(), element);
        }
        reader.close();
        System.out.println(elementMap);

        setCoords(elementList, elementMap);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer(elementMap);
                vis.setVisible(true);
            }
        });
    }

    //col is zero indexed
    public static void setCoords(ArrayList<Element> elementList, HashMap<String, Element> elementMap){
        ArrayList<Integer> colHeights = new ArrayList<>();
        for(Element elem : elementList){
            int col = elem.getMaxColOfInputs(elementMap);
            if(col >= colHeights.size())
                colHeights.add(0);
            int yCoord = colHeights.get(col) + VERT_DIST;
            elem.setOutputPoint(col, yCoord);
            colHeights.set(col, yCoord + elem.getHeight());
        }
    }
}