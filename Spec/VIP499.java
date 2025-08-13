/*    VIP499  */

public class VIP499 {
    public static void Postpaid_register() {
        //voiceAlo_Postpaid=A
        //A--- OCSAIR_voiceAlo_Postpaid_register
        String msisdn = (String) ctx.get("msisdn");//0915043333
        String serviceId = (String) ctx.get("serviceId");//VIP499
        String packageId = (String) ctx.get("packageId");//5600
        String cycleLength = (String) ctx.get("cycleLength");//18 month
        String registerOfferId = (String) ctx.get("registerOfferId");//1100000
        String registerRefillProfileId = (String) ctx.get("registerRefillProfileId");//ALMA
        String registerPamIndicator = (String) ctx.get("registerPamIndicator");//9001
        //communityDataShare_XforY_Postpaid=B
        //B--- OCSAIR_communityDataShare_XforY_Postpaid_register
        String msisdn = (String) ctx.get("msisdn");//0915043333
        String serviceId = (String) ctx.get("serviceId");//VIP499
        String cycle = (String) ctx.get("cycle");
        String memberType = (String) ctx.get("memberType");//Owner
        String volume = (String) ctx.get("volume");//8 GB
        String cycleLength = (String) ctx.get("cycleLength");//1 month
        String daId = (String) ctx.get("daId");//1510
        String daUnitType = (String) ctx.get("daUnitType");//6
        String linkOfferId = (String) ctx.get("linkOfferId");//800072
        String shareOfferId = (String) ctx.get("shareOfferId");//810072
        String dailyOfferId= (String) ctx.get("dailyOfferId");//1000510
        String usageThresholdId= (String) ctx.get("usageThresholdId");//80000510
        String usageCounterId= (String) ctx.get("usageCounterId");//8000051
        //communityCallFree_Postpaid =C+D
        // C--- SOI_communityCreate_X_Postpaid --->
        String msisdn = (String) ctx.get("msisdn");//0915043333
        String serviceId = (String) ctx.get("serviceId");//VIP499
        // D--- OCSAIR_communityCallFree_Postpaid --->
        String msisdn = (String) ctx.get("msisdn");//0915043333
        String serviceId = (String) ctx.get("serviceId");//VIP499
  }
  public static void Postpaid_deregister() {
  }
}
   
