package org.acme;

public class SelledProduct {

	public enum TypeOfAnalysis {
		LOYALTY_CARD, DISCOUNT_COUPON, SHOP, POSTAL_CODE
	}

	public TypeOfAnalysis typeOfAnalysis;
	public String typeValue;
	public java.time.LocalDateTime timestamp;
	public ProductData[] data;

	public SelledProduct() {
	}

	public SelledProduct(TypeOfAnalysis typeOfAnalysis, String typeValue, java.time.LocalDateTime timestamp,
			ProductData[] data) {
		this.typeOfAnalysis = typeOfAnalysis;
		this.typeValue = typeValue;
		this.timestamp = timestamp;
		this.data = data;
	}

	@Override
	public String toString() {
		String string = "{typeOfAnalysis:" + typeOfAnalysis + ", typeValue:" + typeValue
				+ ", timestamp:" + timestamp.toString() + ", data:";

		for (ProductData productData : data) {
			string += productData.toString() + ", ";
		}
		if (data.length > 0) {
			string = string.substring(0, string.length() - 2); // Remove last comma and space
		}

		string += "}";

		return string;
	}

	public static class ProductData {
		public String product;
		public int count;
		public double sumPrice;

		@Override
		public String toString() {
			return "{product:" + product + ", count:" + count + ", sumPrice:" + sumPrice + "}";
		}
	}
}
