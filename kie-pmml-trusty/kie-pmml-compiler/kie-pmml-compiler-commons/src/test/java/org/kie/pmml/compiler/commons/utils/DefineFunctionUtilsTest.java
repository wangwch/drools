/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.pmml.compiler.commons.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.dmg.pmml.Aggregate;
import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.Discretize;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Lag;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.NormDiscrete;
import org.dmg.pmml.ParameterField;
import org.dmg.pmml.TextIndex;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.junit.Test;
import org.kie.pmml.commons.exceptions.KiePMMLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.kie.pmml.compiler.commons.testutils.PMMLModelTestUtils.getParameterFields;
import static org.kie.pmml.compiler.commons.utils.ExpressionFunctionUtilsTest.applySupplier;
import static org.kie.pmml.compiler.commons.utils.ExpressionFunctionUtilsTest.constantSupplier;
import static org.kie.pmml.compiler.commons.utils.ExpressionFunctionUtilsTest.fieldRefSupplier;
import static org.kie.pmml.compiler.commons.utils.ExpressionFunctionUtilsTest.supportedExpressionSupplier;
import static org.kie.pmml.compiler.commons.utils.ExpressionFunctionUtilsTest.unsupportedExpressionSupplier;

public class DefineFunctionUtilsTest {

    private static final Function<Supplier<Expression>, DefineFunction> defineFunctionCreator = supplier -> {
        Expression expression = supplier.get();
        DefineFunction defineFunction = new DefineFunction();
        defineFunction.setName("DEFINE_FUNCTION_" + expression.getClass().getSimpleName());
        defineFunction.setExpression(expression);
        return defineFunction;
    };

    @Test(expected = KiePMMLException.class)
    public void getDefineFunctionsMethodMapUnsupportedExpression() {
        List<DefineFunction> defineFunctions = unsupportedExpressionSupplier.stream().map(defineFunctionCreator).collect(Collectors.toList());
        DefineFunctionUtils.getDefineFunctionsMethodMap(defineFunctions);
    }

    @Test
    public void getDefineFunctionsMethodMapSupportedExpression() {
        List<DefineFunction> defineFunctions = supportedExpressionSupplier.stream().map(defineFunctionCreator).collect(Collectors.toList());
        Map<String, MethodDeclaration> retrieved = DefineFunctionUtils.getDefineFunctionsMethodMap(defineFunctions);
        assertEquals(defineFunctions.size(), retrieved.size());
    }

    @Test
    public void getDefineFunctionMethodDeclarationUnsupportedExpression() {
        for (Supplier<Expression> supplier : unsupportedExpressionSupplier) {
            DefineFunction defineFunction = defineFunctionCreator.apply(supplier);
            try {
                DefineFunctionUtils.getDefineFunctionMethodDeclaration(defineFunction);
                fail(String.format("Expecting KiePMMLException for %s", defineFunction));
            } catch (Exception e) {
                assertEquals(KiePMMLException.class, e.getClass());
            }
        }
    }

    @Test
    public void getDefineFunctionMethodDeclarationSupportedExpression() {
        for (Supplier<Expression> supplier : supportedExpressionSupplier) {
            DefineFunction defineFunction = defineFunctionCreator.apply(supplier);
            try {
                DefineFunctionUtils.getDefineFunctionMethodDeclaration(defineFunction);
            } catch (Exception e) {
                fail(String.format("Unexpected %s for %s", e, defineFunction.getExpression().getClass()));
            }
        }
    }

    @Test(expected = KiePMMLException.class)
    public void getDefineFunctionMethodDeclarationWithoutExpression() {
        DefineFunctionUtils.getDefineFunctionMethodDeclaration(new DefineFunction());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExpressionMethodDeclarationUnknownExpression() {
        Expression expression = new Expression() {
            @Override
            public VisitorAction accept(Visitor visitor) {
                return null;
            }
        };
        DefineFunctionUtils.getExpressionMethodDeclaration("", expression, Collections.emptyList());
    }

    @Test
    public void getExpressionMethodDeclarationUnsupportedExpression() {
        for (Supplier<Expression> supplier : unsupportedExpressionSupplier) {
            Expression expression = supplier.get();
            try {
                DefineFunctionUtils.getExpressionMethodDeclaration("", expression, Collections.emptyList());
                fail(String.format("Expecting KiePMMLException for %s", expression.getClass()));
            } catch (Exception e) {
                assertEquals(KiePMMLException.class, e.getClass());
            }
        }
    }

    @Test
    public void getExpressionMethodDeclarationSupportedExpression() {
        for (Supplier<Expression> supplier : supportedExpressionSupplier) {
            Expression expression = supplier.get();
            try {
                DefineFunctionUtils.getExpressionMethodDeclaration("METHOD_NAME", expression, Collections.emptyList());
            } catch (Exception e) {
                fail(String.format("Unexpected %s for %s", e, expression.getClass()));
            }
        }
    }

    @Test(expected = KiePMMLException.class)
    public void getAggregatedMethodDeclaration() {
        DefineFunctionUtils.getAggregatedMethodDeclaration("", new Aggregate(), Collections.emptyList());
    }

    @Test
    public void getApplyMethodDeclaration() {
        String methodName = "METHOD_NAME";
        MethodDeclaration retrieved = DefineFunctionUtils.getApplyMethodDeclaration(methodName, applySupplier.get(), Collections.emptyList());
        String expected = String.format("java.lang.Object %s() {\n" +
                                                "    java.lang.Object variableapplyVariableConstant1 = 34.6;\n" +
                                                "    java.util.Optional<org.kie.pmml.commons.model.tuples.KiePMMLNameValue> kiePMMLNameValue = param1.stream().filter((org.kie.pmml.commons.model.tuples.KiePMMLNameValue lmbdParam) -> java.util.Objects.equals(\"FIELD_REF\", lmbdParam.getName())).findFirst();\n" +
                                                "    java.lang.Object variableapplyVariableFieldRef2 = kiePMMLNameValue.map(org.kie.pmml.commons.model.tuples.KiePMMLNameValue::getValue).orElse(null);\n" +
                                                "    java.lang.Object applyVariable = this.FUNCTION_NAME(param1, variableapplyVariableConstant1, variableapplyVariableFieldRef2);\n" +
                                                "    return applyVariable;\n" +
                                                "}", methodName);
        assertEquals(expected, retrieved.toString());
    }

    @Test
    public void getConstantMethodDeclaration() {
        String methodName = "METHOD_NAME";
        MethodDeclaration retrieved = DefineFunctionUtils.getConstantMethodDeclaration(methodName, constantSupplier.get(), Collections.emptyList());
        String expected = String.format("java.lang.Double %s() {\n" +
                                                "    java.lang.Double constantVariable = 34.6;\n" +
                                                "    return constantVariable;\n" +
                                                "}", methodName);
        assertEquals(expected, retrieved.toString());
    }

    @Test(expected = KiePMMLException.class)
    public void getDiscretizeMethodDeclaration() {
        DefineFunctionUtils.getDiscretizeMethodDeclaration("", new Discretize(), Collections.emptyList());
    }

    @Test
    public void getFieldRefMethodDeclaration() {
        String methodName = "METHOD_NAME";
        MethodDeclaration retrieved = DefineFunctionUtils.getFieldRefMethodDeclaration(methodName, fieldRefSupplier.get(), Collections.emptyList());
        String expected = String.format("java.lang.Object %s() {\n" +
                                                "    java.util.Optional<org.kie.pmml.commons.model.tuples.KiePMMLNameValue> kiePMMLNameValue = param1.stream().filter((org.kie.pmml.commons.model.tuples.KiePMMLNameValue lmbdParam) -> java.util.Objects.equals(\"FIELD_REF\", lmbdParam.getName())).findFirst();\n" +
                                                "    java.lang.Object fieldRefVariable = kiePMMLNameValue.map(org.kie.pmml.commons.model.tuples.KiePMMLNameValue::getValue).orElse(null);\n" +
                                                "    return fieldRefVariable;\n" +
                                                "}", methodName);
        assertEquals(expected, retrieved.toString());
    }

    @Test(expected = KiePMMLException.class)
    public void getLagMethodDeclaration() {
        DefineFunctionUtils.getLagMethodDeclaration("", new Lag(), Collections.emptyList());
    }

    @Test(expected = KiePMMLException.class)
    public void getMapValuesMethodDeclaration() {
        DefineFunctionUtils.getMapValuesMethodDeclaration("", new MapValues(), Collections.emptyList());
    }

    @Test(expected = KiePMMLException.class)
    public void getNormContinuousMethodDeclaration() {
        DefineFunctionUtils.getNormContinuousMethodDeclaration("", new NormContinuous(), Collections.emptyList());
    }

    @Test(expected = KiePMMLException.class)
    public void getNormDiscreteMethodDeclaration() {
        DefineFunctionUtils.getNormDiscreteMethodDeclaration("", new NormDiscrete(), Collections.emptyList());
    }

    @Test(expected = KiePMMLException.class)
    public void getTextIndexMethodDeclaration() {
        DefineFunctionUtils.getTextIndexMethodDeclaration("", new TextIndex(), Collections.emptyList());
    }

    @Test
    public void getClassOrInterfaceTypes() {
        List<ParameterField> parameterFields = getParameterFields();
        List<ClassOrInterfaceType> retrieved = DefineFunctionUtils.getClassOrInterfaceTypes(parameterFields);
        assertEquals(parameterFields.size(), retrieved.size());
        for (int i = 0; i < parameterFields.size(); i++) {
            commonVerifyParameterClassOrInterfaceType(retrieved.get(i), parameterFields.get(i));
        }
    }

    private void commonVerifyParameterClassOrInterfaceType(ClassOrInterfaceType toVerify, ParameterField parameterField) {
        String expectedClass = ModelUtils.getBoxedClassName(parameterField);
        assertEquals(expectedClass, toVerify.toString());
    }
}