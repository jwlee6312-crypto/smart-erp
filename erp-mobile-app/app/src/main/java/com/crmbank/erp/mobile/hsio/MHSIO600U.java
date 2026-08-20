package com.crmbank.erp.mobile.hsio;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHSIO600U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "자재불출요청 출고"; }
    @Override protected String getProgramId() { return "MHSIO_600U"; }
}
