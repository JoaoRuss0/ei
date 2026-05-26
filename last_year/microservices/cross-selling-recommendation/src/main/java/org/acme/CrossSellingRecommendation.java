package org.acme;

public class CrossSellingRecommendation {

	public Long idLoyaltyCard;
	public Long[] idsShops;

	public CrossSellingRecommendation() {
	}

	public CrossSellingRecommendation(Long idLoyaltyCard, Long[] idsShops) {
		this.idLoyaltyCard = idLoyaltyCard;
		this.idsShops = idsShops;
	}

	@Override
	public String toString() {
		return "{idLoyaltyCard=" + idLoyaltyCard + ", idsShops="
				+ (idsShops != null ? java.util.Arrays.toString(idsShops) : "null") + "}";
	}

}
