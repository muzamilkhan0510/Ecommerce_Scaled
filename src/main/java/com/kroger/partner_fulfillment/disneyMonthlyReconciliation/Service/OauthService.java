package com.kroger.partner_fulfillment.disneyMonthlyReconciliation.Service;

import java.io.IOException;

public interface OauthService {
    String getOauthToken() throws IOException, InterruptedException;
}
