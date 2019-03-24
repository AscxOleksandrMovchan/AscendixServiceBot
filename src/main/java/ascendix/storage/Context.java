package ascendix.storage;

import ascendix.data.Plate;
import ascendix.data.Step;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Context {
    private Step currentStep;
    private Step step;
    private Stack<Step> previousSteps = new Stack<Step>();

    private List<Plate> bucket = new ArrayList<Plate>();

    public Context(Step step) {
        this.step = step;
        currentStep = step;
    }

    public void invokeCommand(String command) {
        if (command.equals("/back") )
            executeBack();
        else if (command.equals("/submit") || command.equals("/mailTo"))
            executeSubmit();
        else
            executeNextStep(command);
    }

    public void reinit() {
        executeSubmit();
    }

    private void executeBack() {
        if (previousSteps.empty()) return;
        currentStep = previousSteps.pop();
    }

    private void executeSubmit() {
        previousSteps = new Stack<Step>();
        bucket = new ArrayList<Plate>();
        currentStep = step;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    private void executeNextStep(String command) {
        for (Step step: currentStep.getSteps())
            if (step.getCommand().equals(command)) {
                previousSteps.push(currentStep);
                currentStep = step;
            }
    }

    public List<Plate> getBucket() {
        return bucket;
    }
}
