package io.cloudbeat.common.wrapper.restassured;

import io.cloudbeat.common.CbTestContext;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public class RestAssuredFailureListener implements ResponseValidationFailureListener {
    final CbTestContext ctx;
    public RestAssuredFailureListener(CbTestContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onFailure(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, Response response) {

    }
}
