package org.apache.cloudstack.quota.vo;

import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ResourcesToQuoteVo {

    private String id;
    private String usageType;
    private int volumeToQuote;
    private PresetVariables metadata;

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

    public int getVolumeToQuote() {
        return volumeToQuote;
    }

    public void setVolumeToQuote(int volumeToQuote) {
        this.volumeToQuote = volumeToQuote;
    }

    public PresetVariables getMetadata() {
        return metadata;
    }

    public void setMetadata(PresetVariables metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
