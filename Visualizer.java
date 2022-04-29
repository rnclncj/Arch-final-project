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
        colNum = -1;
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
        int max = -1;
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

    public int getYBase() {
        return yCoord + getHeight();
    }

    public void draw(Graphics g, HashMap<String, Element> elementMap) {
        Graphics2D g2d = (Graphics2D) g;

        if (getOperation().equals("=") && !getType().equals("reg")) {
            g2d.drawLine(getXCoord(), getYCoord() + Visualizer.BASE_HEIGHT/2, getXCoord() + Visualizer.WIDTH, getYCoord() + Visualizer.BASE_HEIGHT/2);
        } else {
            g2d.drawRect(getXCoord(), getYCoord(), Visualizer.WIDTH, getHeight());
            if(type.equals("reg")){
                int base = getYBase();
                int peakY = base - Visualizer.WIDTH / 6;
                int peakX = getXCoord() + Visualizer.WIDTH/2;
                g2d.drawLine(peakX, peakY, getXCoord() + Visualizer.WIDTH/3, base);
                g2d.drawLine(peakX, peakY, getXCoord() + 2*Visualizer.WIDTH/3, base);
            }
        }

        if(getOperation().equals("<-")){
            g2d.drawString(getOperands().get(0), getXCoord() + Visualizer.WIDTH / 2 - 5, getYCoord() + getHeight()/2-5);
        }
        else{
            g2d.drawString(getOperation(), getXCoord() + Visualizer.WIDTH / 2 - 5, getYCoord() + getHeight()/2 - 5);
        }
        g2d.drawString(getName(), getXCoord() + Visualizer.WIDTH / 2 - 5, getYCoord() + getHeight()/2 + 10);
        

        // input wires
        if (!getOperation().equals("<-")) {
            ArrayList<String> operands = getOperands();
            for (int i = 0; i < operands.size(); i++) {
                int operandX = getXCoord();
                int operandY = getYCoord() + (int) (Visualizer.BASE_HEIGHT * (i + 0.5));
                int inputX = elementMap.get(operands.get(i)).getOutX();
                int inputY = elementMap.get(operands.get(i)).getOutY();
                g2d.drawLine(inputX, inputY, operandX, operandY);
            }
        }
    }
}

class Panel extends JPanel {
    private HashMap<String, Element> elementMap;

    public Panel(HashMap<String, Element> em) {
        elementMap = em;
    }
    
    private void doDrawing(Graphics g) {
        for (Element element : elementMap.values()) {
            element.draw(g, elementMap);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}

public class Visualizer extends JFrame {
    private static final int ADDITIONAL_WIDTH = 4;
    private static final int ADDITIONAL_HEIGHT = 32;

    public static final int BASE_HEIGHT = 30;
    public static final int WIDTH = 50;
    public static final int VERT_DIST = 20;
    public static final int HORIZ_DIST = 40;

    public Visualizer(HashMap<String, Element> em, Dimension dim) {
        initUI(em, dim);
    }

    private void initUI(HashMap<String, Element> em, Dimension dim) {
        Panel panel = new Panel(em);
        panel.setPreferredSize(dim);
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize((int) dim.getWidth() + ADDITIONAL_WIDTH, (int) dim.getHeight() + ADDITIONAL_HEIGHT);
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

        Dimension dim = setCoords(elementList, elementMap);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer(elementMap, dim);
                vis.setVisible(true);
            }
        });
    }

    //col is zero indexed
    public static Dimension setCoords(ArrayList<Element> elementList, HashMap<String, Element> elementMap){
        ArrayList<Integer> colHeights = new ArrayList<>();
        int maxHeight = 0;
        for (Element elem : elementList){
            int col = elem.getMaxColOfInputs(elementMap) + 1;
            if(col >= colHeights.size())
                colHeights.add(0);
            int yCoord = colHeights.get(col) + VERT_DIST;
            elem.setOutputPoint(col, yCoord);
            int newHeight = yCoord + elem.getHeight();
            colHeights.set(col, newHeight);
            maxHeight = Math.max(maxHeight, newHeight);
        }
        int xCoord = colHeights.size() * (Visualizer.HORIZ_DIST + Visualizer.WIDTH) + Visualizer.HORIZ_DIST;
        int yCoord = maxHeight + Visualizer.VERT_DIST;
        return new Dimension(xCoord, yCoord);
        // return new Dimension(xCoord + 20, yCoord + 20);
    }
}