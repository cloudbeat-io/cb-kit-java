package io.cloudbeat.qaf;

import com.google.gson.Gson;
import com.qmetry.qaf.automation.step.QAFTestStepListener;
import com.qmetry.qaf.automation.step.StepExecutionTracker;
import com.qmetry.qaf.automation.step.TestStep;
import com.qmetry.qaf.automation.step.client.text.BDDDefinitionHelper;
import io.cloudbeat.common.CbTestContext;
import io.cloudbeat.common.reporter.CbTestReporter;
import io.cloudbeat.common.reporter.model.StepResult;
import io.cloudbeat.common.reporter.model.TestStatus;
import org.apache.commons.lang.text.StrSubstitutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

public class CbTestStepListener implements QAFTestStepListener {
    private final boolean isActive;
    private final CbTestReporter reporter;
    private StepResult currentStep;

    public CbTestStepListener() {
        isActive = CbTestContext.getInstance().isActive()
                && CbTestContext.getInstance().getReporter() != null
                && CbTestContext.getInstance().getReporter().isStarted();
        reporter = CbTestContext.getInstance().getReporter();
    }
    @Override
    public void beforExecute(StepExecutionTracker stepExecutionTracker) {
        if (!isActive)
            return;
        String stepDescription = getProcessStepDescription(stepExecutionTracker.getStep());
        String msg = "BEGIN STEP: " + stepDescription;
        StepResult cbStep = reporter.startStep(stepDescription);
        stepExecutionTracker.getContext().put("__cbStep", cbStep);
    }

    @Override
    public void afterExecute(StepExecutionTracker stepExecutionTracker) {
        if (!isActive || !stepExecutionTracker.getContext().containsKey("__cbStep"))
            return;
        StepResult cbStep = (StepResult)stepExecutionTracker.getContext().get("__cbStep");
        reporter.endStep(cbStep, TestStatus.PASSED, null, null);
    }

    @Override
    public void onFailure(StepExecutionTracker stepExecutionTracker) {
        if (!isActive || !stepExecutionTracker.getContext().containsKey("__cbStep"))
            return;
        StepResult cbStep = (StepResult)stepExecutionTracker.getContext().get("__cbStep");
        reporter.endStep(cbStep, TestStatus.FAILED, stepExecutionTracker.getException(), null);
    }
    private static List<String> getArgNames(String def) {
        // Pattern p = Pattern.compile("[$][{](.*?)}");
        // Pattern p = Pattern.compile("\"(.*?)[$][{](.*?)}\"");
        // String allChars = "[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]";
        Pattern p = Pattern.compile(
                "\\\"([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\\\\|,.<>\\/? ]*)[${](([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\\\\|,.<>\\/? ]*))}([a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\\\\|,.<>\\/? ]*)\\\"");
        Matcher matcher = p.matcher(def);
        List<String> args = new ArrayList<String>();
        while (matcher.find()) {
            String paramName = matcher.group();
            String finalParamNam = paramName.substring(1, paramName.length() - 2);
            args.add(finalParamNam.replace("${", "{"));
        }
        return args;
    }
    private String getProcessStepDescription(TestStep step) {
        // process parameters in step;

        String description = step.getDescription();

        // if (step instanceof CustomStep) {

        Object[] actualArgs = step.getActualArgs();
        String def = step.getDescription();

        if ((actualArgs != null) && (actualArgs.length > 0)) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.putAll(step.getStepExecutionTracker().getContext());
            List<String> paramNames = getArgNames(def);

            System.out.println(paramNames);

            if ((paramNames != null) && (!paramNames.isEmpty())) {

                for (int i = 0; i < paramNames.size(); i++) {
                    String paramName = paramNames.get(i).trim();
                    // remove starting { and ending } from parameter name
                    paramName = paramName.substring(1, paramName.length() - 1).split(":", 2)[0];

                    // in case of data driven test args[0] should not be overriden
                    // with steps args[0]
                    if ((actualArgs[i] instanceof String)) {

                        String pstr = (String) actualArgs[i];

                        if (pstr.startsWith("${") && pstr.endsWith("}")) {
                            String pname = pstr.substring(2, pstr.length() - 1);
                            actualArgs[i] = paramMap.containsKey(pstr) ? paramMap.get(pstr)
                                    : paramMap.containsKey(pname) ? paramMap.get(pname)
                                    : getBundle().containsKey(pstr) ? getBundle().getObject(pstr)
                                    : getBundle().getObject(pname);
                        } else if (pstr.indexOf("$") >= 0) {
                            pstr = getBundle().getSubstitutor().replace(pstr);
                            actualArgs[i] = StrSubstitutor.replace(pstr, paramMap);
                        }
                        // continue;
                        BDDDefinitionHelper.ParamType ptype = BDDDefinitionHelper.ParamType.getType(pstr);
                        if (ptype.equals(BDDDefinitionHelper.ParamType.MAP)) {
                            Map<String, Object> kv = new Gson().fromJson(pstr, Map.class);
                            paramMap.put(paramName, kv);
                            for (String key : kv.keySet()) {
                                paramMap.put(paramName + "." + key, kv.get(key));
                            }
                        } else if (ptype.equals(BDDDefinitionHelper.ParamType.LIST)) {
                            List<Object> lst = new Gson().fromJson(pstr, List.class);
                            paramMap.put(paramName, lst);
                            for (int li = 0; li < lst.size(); li++) {
                                paramMap.put(paramName + "[" + li + "]", lst.get(li));
                            }
                        }
                    }

                    paramMap.put("${args[" + i + "]}", actualArgs[i]);
                    paramMap.put("args[" + i + "]", actualArgs[i]);
                    paramMap.put(paramName, actualArgs[i]);

                }

                description = StrSubstitutor.replace(description, paramMap);

            }
        }
        return description;
    }

}
