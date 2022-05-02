import java.awt.*;
import java.util.*;

public class Element {
    private String name;
    private String type;
    private int width;
    private String operation;
    private ArrayList<String> operands;
    private int colNum;
    private int yCoord;

    // constructor from file line
    public Element(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        type = tokenizer.nextToken();
        name = tokenizer.nextToken();
        width = 1;
        if(name.contains("]")){
            width = Integer.parseInt(name.substring(name.indexOf("[")+1, name.indexOf("]")));
            name = name.substring(name.indexOf("]")+1);
        }
        operation = tokenizer.nextToken();
        operands = new ArrayList<>();
        colNum = -1;
        yCoord = 0;
        while (tokenizer.hasMoreTokens()) {
            operands.add(tokenizer.nextToken());
        }
    }

    // propagation constructor
    public Element(Element elem, int cn, boolean isForward){
        this.name = elem.name;
        this.type = elem.type;
        width = elem.width;
        if (isForward) {
            operation = "->";
        } else {
            operation = "<-";
        }
        operands = new ArrayList<>();
        operands.add(name);
        colNum = cn;
    }

    public String toString() {
        String str = name + " " + type + " " + operation + " " + operands;
        return str;
    }

    public int getMaxColOfInputs(HashMap<String, Element> elementMap) {
        int max = -1;
        for (String input : operands) {
            if (!elementMap.containsKey(input))
                continue;
            int currCol = elementMap.get(input).getColNum();
            max = Math.max(currCol, max);
        }
        return max;
    }

    public int getXCoord() {
        return Visualizer.HORIZ_DIST + getColNum() * (Visualizer.FULL_WIDTH + Visualizer.HORIZ_DIST);
    }

    // returns the height of the element
    public int getHeight() {
        if (operation.equals("->") || operation.equals("<-"))
            return Visualizer.BASE_HEIGHT / 4;
        return Visualizer.BASE_HEIGHT * getOperands().size();
    }

    // returns the height of the element
    public int getRealBaseHeight() {
        return getHeight() / operands.size();
    }

    public int getOutX() {
        return getXCoord() + Visualizer.FULL_WIDTH;
    }

    public int getOutY() {
        return yCoord + getHeight() / 2;
    }

    public void setYCoord(int yCoord) {
        this.yCoord = yCoord;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public ArrayList<String> getOperands() {
        return operands;
    }

    public void setOutputPoint(int colNum, int yCoord) {
        this.colNum = colNum;
        this.yCoord = yCoord;
    }

    public int getColNum() {
        return colNum;
    }

    public void setColNum(int colNum) {
        this.colNum = colNum;
    }

    public int getYCoord() {
        return yCoord;
    }

    public int getOperandYCoord(int ind) {
        return yCoord + (int) (getRealBaseHeight() * (ind + 0.5));
    }

    public int getYBase() {
        return yCoord + getHeight();
    }

    public int getWidth(){
        return width;
    }

    public void setWidth(int width){
        this.width = width;
    }

    private void drawStringCentered(Graphics2D g2d, String str, int x, int y){
        int width =  g2d.getFontMetrics().stringWidth(str);
        g2d.drawString(str, x - width / 2, y);
    }

    public void drawGate(Graphics2D g2d){
        if ((getOperation().equals("=") && !getType().equals("reg")) || getOperation().equals("->") || getOperation().equals("<-")) {
            g2d.drawLine(getXCoord(), getYCoord() + getHeight() / 2, getXCoord() + Visualizer.BOX_WIDTH,
                    getYCoord() + getHeight() / 2);
        } else if(getOperation().equals("?:")){
            int[] xPoints = {getXCoord(),getXCoord()+Visualizer.BOX_WIDTH*3/4,getXCoord()+Visualizer.BOX_WIDTH*3/4, getXCoord()};
            int height = getHeight() - Visualizer.BASE_HEIGHT;
            int yCorner = getYCoord() + Visualizer.BASE_HEIGHT;
            int[] yPoints = {yCorner, yCorner + height / 3,yCorner + 2 * height / 3 , yCorner + height};
            g2d.drawPolygon(xPoints,yPoints, 4);
            int yIn = getYCoord() + Visualizer.BASE_HEIGHT / 2;
            g2d.drawLine(getXCoord(), yIn, getXCoord() + Visualizer.BOX_WIDTH * 3 / 8, yIn);
            g2d.drawLine(getXCoord() + Visualizer.BOX_WIDTH * 3 / 8, yIn, getXCoord() + Visualizer.BOX_WIDTH * 3 / 8, yCorner + height / 6);
            g2d.drawLine(getXCoord() + Visualizer.BOX_WIDTH * 3 / 4 , yCorner + height  * 3 / 4, getXCoord() + Visualizer.BOX_WIDTH, getYCoord() + getHeight() / 2);
        }
        else if (!getOperation().equals("--")) {
            g2d.drawRect(getXCoord(), getYCoord(), Visualizer.BOX_WIDTH, getHeight()); //draws box
            if (type.equals("reg")) {
                int base = getYBase();
                int peakY = base - Visualizer.BOX_WIDTH / 6;
                int peakX = getXCoord() + Visualizer.BOX_WIDTH / 2;
                g2d.drawLine(peakX, peakY, getXCoord() + Visualizer.BOX_WIDTH / 3, base);
                g2d.drawLine(peakX, peakY, getXCoord() + 2 * Visualizer.BOX_WIDTH / 3, base);
            }
        }
    }

    public void draw(Graphics2D g2d, HashMap<String, Element> elementMap, ArrayList<HashMap<String, Element>>[] columnMaps) {
        if (getOperation().equals("<-")) {
            g2d.setColor(Visualizer.BACKWARDS_COLOR);
        }

        drawGate(g2d);
        
        if (getOperation().equals("--")) {
            g2d.setFont(new Font(g2d.getFont().getName(), Font.PLAIN, 9)); 
            drawStringCentered(g2d, getOperands().get(0), getXCoord() + Visualizer.FULL_WIDTH / 2, getYCoord() + getHeight() / 2 - 3);
            g2d.drawLine(getXCoord(), getYCoord() + getHeight() / 2, getXCoord() + Visualizer.FULL_WIDTH,
                    getYCoord() + getHeight() / 2); //draws full line
            g2d.drawLine(getXCoord(), getYCoord() + getHeight() / 2, getXCoord(), getYCoord() + getHeight() / 2 - 7);
        } else if (!(getOperation().equals("=") || getOperation().equals("->") || getOperation().equals("<-"))) {
            g2d.setFont(new Font(g2d.getFont().getName(), Font.PLAIN, 12)); 
            drawStringCentered(g2d, getOperation(), getXCoord() + Visualizer.BOX_WIDTH / 2, getYCoord() + getHeight() / 2 + 4);
        }
        //draw stud line
        g2d.drawLine(getXCoord()+Visualizer.BOX_WIDTH, getYCoord() + getHeight() / 2, getXCoord() + Visualizer.FULL_WIDTH,
                    getYCoord() + getHeight() / 2);
        //draws name
        if (!(getName().charAt(0) == '.' || getOperation().equals("->") || getOperation().equals("<-"))) {
            g2d.setFont(new Font(g2d.getFont().getName(), Font.PLAIN, 9));
            String name = Visualizer.condenseName(getName(), 10);
            drawStringCentered(g2d, name, getXCoord() + Visualizer.BOX_WIDTH + Visualizer.STUD_WIDTH / 2, getYCoord() + getHeight() / 2 + 10); //draw name
            g2d.drawLine(getXCoord()+Visualizer.BOX_WIDTH + Visualizer.STUD_WIDTH / 2 + 1, getYCoord() + getHeight() / 2 - 3, 
                    getXCoord()+Visualizer.BOX_WIDTH + Visualizer.STUD_WIDTH / 2 - 1, getYCoord() + getHeight() / 2 + 2);
            drawStringCentered(g2d, ""+getWidth(), getXCoord() + Visualizer.BOX_WIDTH + Visualizer.STUD_WIDTH / 2, getYCoord() + getHeight() / 2 - 5); //draw name
        }

        // input wires
        if (!getOperation().equals("--")) {
            ArrayList<String> operands = getOperands();
            for (int i = 0; i < operands.size(); i++) {
                int operandX = getXCoord();
                int operandY = getOperandYCoord(i);
                
                // if (elementMap.get(operands.get(i)) == null)
                //     System.out.println(operands.get(i));
                // if backward propagation wire can't connect back anymore, skip this step
                if (getOperation().equals("<-") && !columnMaps[1].get(getColNum() - 1).containsKey(operands.get(i))) {
                    continue;
                }
                // if this input requires a backward wire, skip this step
                if (!getOperation().equals("<-") && elementMap.get(operands.get(i)).getColNum() >= getColNum()) {
                    continue;
                }
                Element fromElem;
                if (getOperation().equals("<-")) {
                    fromElem = columnMaps[1].get(getColNum() - 1).get(operands.get(i));
                } else {
                    fromElem = columnMaps[0].get(getColNum() - 1).get(operands.get(i));
                }
                int inputX = fromElem.getOutX();
                int inputY = fromElem.getOutY();
                g2d.drawLine(inputX, inputY, operandX, operandY);
            }
        }

        g2d.setColor(Color.BLACK);
    }
}