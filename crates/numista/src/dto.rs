use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::BTreeMap;
use std::fmt;

pub type ExtraFields = BTreeMap<String, Value>;

#[derive(Clone, Default, PartialEq, Serialize, Deserialize)]
pub struct OAuthTokenResponse {
    pub access_token: Option<String>,
    pub token_type: Option<String>,
    pub expires_in: Option<u64>,
    pub user_id: Option<u64>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

impl fmt::Debug for OAuthTokenResponse {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("OAuthTokenResponse")
            .field(
                "access_token",
                &self.access_token.as_ref().map(|_| "[REDACTED]"),
            )
            .field("token_type", &self.token_type)
            .field("expires_in", &self.expires_in)
            .field("user_id", &self.user_id)
            .field("extra", &self.extra)
            .finish()
    }
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct CollectedItemsResponse {
    pub item_count: Option<u64>,
    pub item_for_swap_count: Option<u64>,
    pub item_type_count: Option<u64>,
    pub item_type_for_swap_count: Option<u64>,
    pub items: Option<Vec<CollectedItem>>,
    #[serde(flatten)]
    pub extra: ExtraFields,
    /// Exact response body populated by [`crate::NumistaClient`].
    ///
    /// Fixture deserialization leaves this as `None`.
    #[serde(skip)]
    pub raw: Option<Value>,
}

impl CollectedItemsResponse {
    pub fn raw_json(&self) -> Option<&Value> {
        self.raw.as_ref()
    }

    pub fn take_raw_json(&mut self) -> Option<Value> {
        self.raw.take()
    }

    pub fn into_raw_json(self) -> Option<Value> {
        self.raw
    }
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct CollectedItem {
    pub id: Option<u64>,
    pub quantity: Option<u32>,
    #[serde(rename = "type")]
    pub item_type: Option<ItemType>,
    pub issue: Option<Issue>,
    pub grade: Option<String>,
    pub price: Option<Price>,
    pub for_swap: Option<bool>,
    pub collection: Option<Collection>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct ItemType {
    pub id: Option<u32>,
    pub title: Option<String>,
    pub category: Option<String>,
    pub issuer: Option<Issuer>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Issuer {
    pub code: Option<String>,
    pub name: Option<String>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Issue {
    pub id: Option<u64>,
    pub is_dated: Option<bool>,
    pub year: Option<i32>,
    pub gregorian_year: Option<i32>,
    pub mint_letter: Option<String>,
    pub mintage: Option<u64>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Price {
    pub value: Option<f64>,
    pub currency: Option<String>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Collection {
    pub id: Option<u64>,
    pub name: Option<String>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct NumistaType {
    pub id: Option<u32>,
    pub url: Option<String>,
    pub title: Option<String>,
    pub issuer: Option<Issuer>,
    pub min_year: Option<i32>,
    pub max_year: Option<i32>,
    pub weight: Option<f64>,
    pub size: Option<f64>,
    pub size2: Option<f64>,
    pub thickness: Option<f64>,
    pub shape: Option<String>,
    pub orientation: Option<String>,
    pub composition: Option<Composition>,
    pub series: Option<String>,
    #[serde(alias = "commemorated_event")]
    pub commemorated_topic: Option<String>,
    pub references: Option<Vec<CatalogueReference>>,
    pub obverse: Option<CoinSide>,
    pub reverse: Option<CoinSide>,
    pub edge: Option<CoinSide>,
    #[serde(flatten)]
    pub extra: ExtraFields,
    /// Exact response body populated by [`crate::NumistaClient`].
    ///
    /// Fixture deserialization leaves this as `None`.
    #[serde(skip)]
    pub raw: Option<Value>,
}

impl NumistaType {
    pub fn raw_json(&self) -> Option<&Value> {
        self.raw.as_ref()
    }

    pub fn take_raw_json(&mut self) -> Option<Value> {
        self.raw.take()
    }

    pub fn into_raw_json(self) -> Option<Value> {
        self.raw
    }
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Composition {
    pub text: Option<String>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct CoinSide {
    pub description: Option<String>,
    pub lettering: Option<String>,
    pub picture: Option<String>,
    pub thumbnail: Option<String>,
    pub picture_copyright: Option<String>,
    pub picture_copyright_url: Option<String>,
    pub engravers: Option<Vec<String>>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct CatalogueReference {
    pub catalogue: Option<Catalogue>,
    pub number: Option<String>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct Catalogue {
    pub id: Option<u32>,
    pub code: Option<String>,
    #[serde(flatten)]
    pub extra: ExtraFields,
}
