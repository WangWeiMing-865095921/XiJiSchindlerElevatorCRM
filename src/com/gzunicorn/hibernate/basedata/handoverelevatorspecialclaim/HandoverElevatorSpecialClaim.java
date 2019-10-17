package com.gzunicorn.hibernate.basedata.handoverelevatorspecialclaim;



/**
 * HandoverElevatorSpecialClaim entity. @author MyEclipse Persistence Tools
 */
public class HandoverElevatorSpecialClaim extends AbstractHandoverElevatorSpecialClaim implements java.io.Serializable {

    // Constructors

    /** default constructor */
    public HandoverElevatorSpecialClaim() {
    }

	/** minimal constructor */
    public HandoverElevatorSpecialClaim(String scId, String scName, String enabledFlag, String operId, String operDate) {
        super(scId, scName, enabledFlag, operId, operDate);        
    }
    
    /** full constructor */
    public HandoverElevatorSpecialClaim(String scId, String scName, String enabledFlag, String rem, String operId, String operDate, String r1, String r2, String r3, String r4, String r5, Double r6, Double r7, Double r8, Integer r9, Integer r10) {
        super(scId, scName, enabledFlag, rem, operId, operDate, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10);        
    }
   
}
