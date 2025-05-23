package io.cloudbeat.common.client;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public abstract class RetrofitApiBase {
    public <T> T execute(Call<T> call) throws CbClientException {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            }
            if (response.code() == 500) {
                throw new CbClientException("Internal server error");
            }
            if (response.code() == 404) {
                throw new CbClientException("Entity not found");
            }
        } catch (IOException e) {
            throw new CbClientException(e);
        }
        return null;
    }
    public <T> void executeAsync(Call<T> call) throws CbClientException {
        call.enqueue(new Callback<T>()
        {
            @Override
            public void onResponse(Call<T> call, Response<T> response)
            {
            }

            @Override
            public void onFailure(Call<T> call, Throwable t)
            {
                System.err.println("CloudBeat API call failed: ");
                if (t != null)
                    System.err.println(t.toString());
            }
        });
    }
    public <T> T executeWithApiResponse(Call<CbApiResponse<T>> call) throws CbClientException {
        try {
            Response<CbApiResponse<T>> response = call.execute();
            if (response.isSuccessful()) {
                return response.body().data;
            }
            if (response.code() == 500) {
                throw new CbClientException("Internal server error");
            }
            if (response.code() == 404) {
                throw new CbClientException("Entity not found");
            }
        } catch (IOException e) {
            throw new CbClientException(e);
        }
        return null;
    }
}
