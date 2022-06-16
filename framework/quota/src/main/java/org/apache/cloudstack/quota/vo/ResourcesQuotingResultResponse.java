package org.apache.cloudstack.quota.vo;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ResourcesQuotingResultResponse extends BaseResponse {

    @SerializedName("quoteid")
    @Param(description = "Value sent in API to identify the quote")
    private String id;

    @SerializedName("usagetype")
    @Param(description = "Usage type")
    private String usageType;

    @SerializedName("quote")
    @Param(description = "Quote result")
    private BigDecimal quote;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public BigDecimal getQuote() {
        return quote;
    }

    public void setQuote(BigDecimal quote) {
        this.quote = quote.setScale(2, RoundingMode.HALF_EVEN);;
    }

}
