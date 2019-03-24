package ascendix.data;

import java.util.List;

public class Step {
    private String command;
    private String name;
    private String parent;
    private List<Step> steps;

    public Step() {
    }

    public Step(String command, String name, String parent, List<Step> steps) {
        this.command = command;
        this.name = name;
        this.parent = parent;
        this.steps = steps;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }
}
