use std::collections::{BTreeMap, BTreeSet};
use std::fmt;

use serde::{Deserialize, Serialize};
use thiserror::Error;

#[derive(Clone, Debug, Deserialize, Eq, Ord, PartialEq, PartialOrd, Serialize)]
#[serde(transparent)]
pub struct SeriesId(String);

impl SeriesId {
    pub fn new(value: impl Into<String>) -> Self {
        Self(value.into())
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for SeriesId {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.0.fmt(formatter)
    }
}

#[derive(Clone, Debug, Deserialize, Eq, Ord, PartialEq, PartialOrd, Serialize)]
#[serde(transparent)]
pub struct SlotId(String);

impl SlotId {
    pub fn new(value: impl Into<String>) -> Self {
        Self(value.into())
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for SlotId {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.0.fmt(formatter)
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub enum Metal {
    Silver,
    Gold,
    Platinum,
    Other,
}

#[derive(Clone, Debug, Deserialize, Eq, Ord, PartialEq, PartialOrd, Serialize)]
pub enum Finish {
    Bullion,
    Proof,
    Coloured,
    ProofColoured,
    Gilded,
    Antiqued,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub enum ReleaseStatus {
    Issued,
    Announced,
    Expected,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct Matcher {
    #[serde(default)]
    pub issuer_code: Option<String>,
    #[serde(default)]
    pub year: Option<i32>,
    #[serde(default)]
    pub weight_oz: Option<f32>,
    #[serde(default)]
    pub finish: Option<Finish>,
    #[serde(default)]
    pub title_contains: Vec<String>,
    pub confidence: f32,
    pub explanation: String,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct Series {
    pub id: SeriesId,
    pub name: String,
    pub mint: String,
    pub issuer_code: String,
    pub metal: Metal,
    pub notes: Option<String>,
    #[serde(default)]
    pub incomplete: bool,
    #[serde(default)]
    pub sources: BTreeMap<SlotId, String>,
    pub slots: Vec<Slot>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct Slot {
    pub id: SlotId,
    pub label: String,
    pub year: i32,
    pub motif: String,
    pub weight_oz: f32,
    pub finish: Finish,
    pub release_status: ReleaseStatus,
    #[serde(default)]
    pub numista_type_ids: Vec<u32>,
    #[serde(default)]
    pub matchers: Vec<Matcher>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct CollectedItem {
    pub id: u64,
    pub quantity: u32,
    pub type_id: u32,
    #[serde(default)]
    pub title: Option<String>,
    #[serde(default)]
    pub issuer_code: Option<String>,
    #[serde(default)]
    pub issue_year: Option<i32>,
    #[serde(default)]
    pub gregorian_year: Option<i32>,
    #[serde(default)]
    pub grade: Option<String>,
    #[serde(default)]
    pub price: Option<f64>,
    #[serde(default)]
    pub for_swap: Option<bool>,
    #[serde(default)]
    pub collection_name: Option<String>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct TypeMeta {
    pub id: u32,
    #[serde(default)]
    pub title: Option<String>,
    #[serde(default)]
    pub display_title: Option<String>,
    #[serde(default)]
    pub family: Option<String>,
    #[serde(default)]
    pub issuer_code: Option<String>,
    #[serde(default)]
    pub min_year: Option<i32>,
    #[serde(default)]
    pub max_year: Option<i32>,
    #[serde(default)]
    pub weight_oz: Option<f32>,
    #[serde(default)]
    pub finish: Option<Finish>,
}

pub type TypeMetaIndex = BTreeMap<u32, TypeMeta>;

#[derive(Clone, Debug, PartialEq, Serialize)]
pub struct CollectionProposal {
    pub family: String,
    pub weight_millioz: u32,
    pub finish: Option<Finish>,
    pub distinct_types: usize,
    pub quantity: u32,
}

impl CollectionProposal {
    pub fn key(&self) -> CollectionProposalKey {
        CollectionProposalKey {
            family: self.family.clone(),
            weight_millioz: self.weight_millioz,
            finish: self.finish.clone(),
        }
    }
}

#[derive(Clone, Debug, Eq, Ord, PartialEq, PartialOrd)]
pub struct CollectionProposalKey {
    pub family: String,
    pub weight_millioz: u32,
    pub finish: Option<Finish>,
}

impl CollectionProposalKey {
    pub fn from_canonical_parts(
        family: &str,
        weight_millioz: u32,
        finish_code: &str,
    ) -> Option<Self> {
        let normalized = normalize_family(family)?;
        if normalized != family || family.len() > 256 || !(1..=1_000_000).contains(&weight_millioz)
        {
            return None;
        }
        Some(Self {
            family: normalized,
            weight_millioz,
            finish: finish_from_code(finish_code)?,
        })
    }

    pub fn finish_code(&self) -> &'static str {
        finish_code(self.finish.as_ref())
    }
}

#[derive(Clone, Debug, Deserialize, Eq, Ord, PartialEq, PartialOrd, Serialize)]
#[serde(transparent)]
pub struct CollectionCatalogId(String);

impl CollectionCatalogId {
    pub fn new(value: impl Into<String>) -> Self {
        Self(value.into())
    }

    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl fmt::Display for CollectionCatalogId {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.0.fmt(formatter)
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct CollectionCatalog {
    pub schema_version: u32,
    pub id: CollectionCatalogId,
    pub name: String,
    pub issuer_code: String,
    pub family: String,
    pub weight_millioz: u32,
    pub finish: Option<Finish>,
    pub source: String,
    pub updated_at: String,
    pub members: Vec<CollectionCatalogMember>,
}

impl CollectionCatalog {
    pub fn key(&self) -> CollectionProposalKey {
        CollectionProposalKey {
            family: self.family.clone(),
            weight_millioz: self.weight_millioz,
            finish: self.finish.clone(),
        }
    }

    pub fn validate(&self) -> Result<(), CollectionCatalogValidationError> {
        if !matches!(self.schema_version, 1 | 2) {
            return Err(CollectionCatalogValidationError::UnsupportedSchemaVersion {
                version: self.schema_version,
            });
        }
        if !is_slug(self.id.as_str()) {
            return Err(CollectionCatalogValidationError::InvalidId {
                kind: "catalog",
                value: self.id.to_string(),
            });
        }
        validate_collection_catalog_text("catalog.name", &self.name)?;
        validate_collection_catalog_text("catalog.issuer_code", &self.issuer_code)?;
        validate_collection_catalog_text("catalog.updated_at", &self.updated_at)?;
        if CollectionProposalKey::from_canonical_parts(
            &self.family,
            self.weight_millioz,
            finish_code(self.finish.as_ref()),
        )
        .as_ref()
            != Some(&self.key())
        {
            return Err(CollectionCatalogValidationError::InvalidVariantKey);
        }
        let source_is_valid = match self.schema_version {
            1 => is_numista_series_source(&self.source),
            _ => is_numista_series_source(&self.source) || is_numista_type_source(&self.source),
        };
        if !source_is_valid {
            return Err(CollectionCatalogValidationError::InvalidSource);
        }
        if self.members.is_empty() {
            return Err(CollectionCatalogValidationError::EmptyMembers);
        }

        let mut member_ids = BTreeSet::new();
        let mut type_ids = BTreeSet::new();
        let mut date_slots = BTreeSet::new();
        for member in &self.members {
            if !is_slug(&member.id) {
                return Err(CollectionCatalogValidationError::InvalidId {
                    kind: "member",
                    value: member.id.clone(),
                });
            }
            if !member_ids.insert(&member.id) {
                return Err(CollectionCatalogValidationError::DuplicateMemberId {
                    member_id: member.id.clone(),
                });
            }
            validate_collection_catalog_text(
                &format!("member[{}].label", member.id),
                &member.label,
            )?;
            if member.numista_type_id == 0 {
                return Err(CollectionCatalogValidationError::InvalidNumistaTypeId);
            }
            if self.schema_version == 1 {
                if !type_ids.insert(member.numista_type_id) {
                    return Err(CollectionCatalogValidationError::DuplicateNumistaTypeId {
                        type_id: member.numista_type_id,
                    });
                }
            } else if !date_slots.insert((member.numista_type_id, member.year)) {
                return Err(CollectionCatalogValidationError::DuplicateMemberYear {
                    type_id: member.numista_type_id,
                    year: member.year,
                });
            }
        }
        Ok(())
    }

    /// Whether a collected item satisfies one catalog member. Schema 1 catalogs
    /// match by Numista type alone; schema 2 date runs also require the issue
    /// year recorded on the item.
    pub fn member_matches(&self, member: &CollectionCatalogMember, item: &CollectedItem) -> bool {
        item.quantity > 0
            && item.type_id == member.numista_type_id
            && (self.schema_version == 1
                || item.issue_year == Some(member.year)
                || item.gregorian_year == Some(member.year))
    }

    pub fn is_evidenced_by(&self, items: &[CollectedItem]) -> bool {
        let member_type_ids: BTreeSet<u32> = self
            .members
            .iter()
            .map(|member| member.numista_type_id)
            .collect();
        items
            .iter()
            .any(|item| item.quantity > 0 && member_type_ids.contains(&item.type_id))
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(deny_unknown_fields)]
pub struct CollectionCatalogMember {
    pub id: String,
    pub label: String,
    pub year: i32,
    pub numista_type_id: u32,
}

#[derive(Clone, Debug, Error, PartialEq)]
pub enum CollectionCatalogValidationError {
    #[error("collection catalog schema version `{version}` is not supported")]
    UnsupportedSchemaVersion { version: u32 },
    #[error("{kind} id `{value}` must be a nonempty lowercase slug")]
    InvalidId { kind: &'static str, value: String },
    #[error("required field `{field}` cannot be blank")]
    BlankField { field: String },
    #[error("collection catalog has an invalid proposal variant key")]
    InvalidVariantKey,
    #[error(
        "collection catalog source must be an HTTPS Numista series URL (or a Numista type URL for date runs)"
    )]
    InvalidSource,
    #[error("collection catalog must contain at least one member")]
    EmptyMembers,
    #[error("member id `{member_id}` is duplicated")]
    DuplicateMemberId { member_id: String },
    #[error("Numista type id must be greater than zero")]
    InvalidNumistaTypeId,
    #[error("Numista type id `{type_id}` is assigned more than once in the collection catalog")]
    DuplicateNumistaTypeId { type_id: u32 },
    #[error("date-run member for type `{type_id}` and year `{year}` is duplicated")]
    DuplicateMemberYear { type_id: u32, year: i32 },
}

#[derive(Clone, Debug, PartialEq, Serialize)]
pub enum CollectionCatalogMemberStatus {
    Owned { quantity: u32, items: Vec<ItemRef> },
    Missing,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
pub struct CollectionCatalogAlbumMember {
    pub member: CollectionCatalogMember,
    pub status: CollectionCatalogMemberStatus,
}

#[derive(Clone, Debug, PartialEq, Serialize)]
pub struct CollectionCatalogAlbum {
    pub catalog_id: CollectionCatalogId,
    pub name: String,
    pub members: Vec<CollectionCatalogAlbumMember>,
}

impl CollectionCatalogAlbum {
    pub fn owned_members(&self) -> usize {
        self.members
            .iter()
            .filter(|member| matches!(member.status, CollectionCatalogMemberStatus::Owned { .. }))
            .count()
    }
}

pub fn build_collection_catalog_album(
    catalog: &CollectionCatalog,
    items: &[CollectedItem],
) -> CollectionCatalogAlbum {
    CollectionCatalogAlbum {
        catalog_id: catalog.id.clone(),
        name: catalog.name.clone(),
        members: catalog
            .members
            .iter()
            .cloned()
            .map(|member| {
                let owned_items: Vec<ItemRef> = items
                    .iter()
                    .filter(|item| catalog.member_matches(&member, item))
                    .map(|item| ItemRef {
                        item_id: item.id,
                        type_id: item.type_id,
                        quantity: item.quantity,
                        match_source: Some(MatchSource::ExplicitTypeId),
                        unmatched_reason: None,
                    })
                    .collect();
                let quantity = owned_items
                    .iter()
                    .fold(0_u32, |total, item| total.saturating_add(item.quantity));
                let status = if owned_items.is_empty() {
                    CollectionCatalogMemberStatus::Missing
                } else {
                    CollectionCatalogMemberStatus::Owned {
                        quantity,
                        items: owned_items,
                    }
                };
                CollectionCatalogAlbumMember { member, status }
            })
            .collect(),
    }
}

fn validate_collection_catalog_text(
    field: &str,
    value: &str,
) -> Result<(), CollectionCatalogValidationError> {
    if value.trim().is_empty() {
        Err(CollectionCatalogValidationError::BlankField {
            field: field.to_owned(),
        })
    } else {
        Ok(())
    }
}

fn is_numista_series_source(source: &str) -> bool {
    source
        .strip_prefix("https://en.numista.com/catalogue/series.php?id=")
        .is_some_and(|id| !id.is_empty() && id.bytes().all(|byte| byte.is_ascii_digit()))
}

fn is_numista_type_source(source: &str) -> bool {
    source
        .strip_prefix("https://en.numista.com/catalogue/pieces")
        .and_then(|rest| rest.strip_suffix(".html"))
        .is_some_and(|id| !id.is_empty() && id.bytes().all(|byte| byte.is_ascii_digit()))
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ProposalDisposition {
    Followed,
    Ignored,
}

impl ProposalDisposition {
    pub fn from_code(code: &str) -> Option<Self> {
        match code {
            "followed" => Some(Self::Followed),
            "ignored" => Some(Self::Ignored),
            _ => None,
        }
    }

    pub fn as_code(self) -> &'static str {
        match self {
            Self::Followed => "followed",
            Self::Ignored => "ignored",
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CollectionProposalPreference {
    pub key: CollectionProposalKey,
    pub disposition: ProposalDisposition,
}

#[derive(Clone, Debug, Default, PartialEq)]
pub struct ClassifiedCollectionProposals {
    pub followed: Vec<CollectionProposal>,
    pub available: Vec<CollectionProposal>,
    pub ignored: Vec<CollectionProposal>,
}

pub fn classify_collection_proposals(
    proposals: Vec<CollectionProposal>,
    preferences: &[CollectionProposalPreference],
) -> ClassifiedCollectionProposals {
    let preferences: BTreeMap<&CollectionProposalKey, ProposalDisposition> = preferences
        .iter()
        .map(|preference| (&preference.key, preference.disposition))
        .collect();
    let mut classified = ClassifiedCollectionProposals::default();
    for proposal in proposals {
        match preferences.get(&proposal.key()) {
            Some(ProposalDisposition::Followed) => classified.followed.push(proposal),
            Some(ProposalDisposition::Ignored) => classified.ignored.push(proposal),
            None => classified.available.push(proposal),
        }
    }
    classified
}

pub fn normalize_weight_millioz(weight_oz: f32) -> Option<u32> {
    if !weight_oz.is_finite() || weight_oz <= 0.0 {
        return None;
    }
    let measured = (weight_oz * 1_000.0).round() as u32;
    [250, 500, 1_000, 2_000, 5_000, 10_000]
        .into_iter()
        .find(|common| measured.abs_diff(*common) <= 10)
        .or(Some(measured))
}

pub fn build_collection_proposals(
    items: &[CollectedItem],
    type_meta: &TypeMetaIndex,
    catalogs: &[CollectionCatalog],
) -> Vec<CollectionProposal> {
    let catalog_families: BTreeMap<u32, &str> = catalogs
        .iter()
        .flat_map(|catalog| {
            catalog
                .members
                .iter()
                .map(|member| (member.numista_type_id, catalog.family.as_str()))
        })
        .collect();
    let mut grouped: BTreeMap<(String, u32, u8), ProposalAccumulator> = BTreeMap::new();

    for item in items.iter().filter(|item| item.quantity > 0) {
        let Some(metadata) = type_meta.get(&item.type_id) else {
            continue;
        };
        let numista_family = metadata.family.as_deref().and_then(normalize_family);
        if numista_family.as_deref().is_some_and(is_technical_family) {
            continue;
        }
        let Some(family) = numista_family.or_else(|| {
            catalog_families
                .get(&item.type_id)
                .map(|family| (*family).to_owned())
        }) else {
            continue;
        };
        let Some(weight_millioz) = metadata.weight_oz.and_then(normalize_weight_millioz) else {
            continue;
        };
        let key = (
            family.clone(),
            weight_millioz,
            finish_key(metadata.finish.as_ref()),
        );
        let accumulator = grouped.entry(key).or_insert_with(|| ProposalAccumulator {
            proposal: CollectionProposal {
                family,
                weight_millioz,
                finish: metadata.finish.clone(),
                distinct_types: 0,
                quantity: 0,
            },
            type_ids: BTreeSet::new(),
        });
        accumulator.type_ids.insert(item.type_id);
        accumulator.proposal.quantity = accumulator.proposal.quantity.saturating_add(item.quantity);
    }

    grouped
        .into_values()
        .map(|mut accumulator| {
            accumulator.proposal.distinct_types = accumulator.type_ids.len();
            accumulator.proposal
        })
        .collect()
}

struct ProposalAccumulator {
    proposal: CollectionProposal,
    type_ids: BTreeSet<u32>,
}

fn normalize_family(family: &str) -> Option<String> {
    let normalized = family.split_whitespace().collect::<Vec<_>>().join(" ");
    (!normalized.is_empty()).then_some(normalized)
}

pub fn collection_proposal_family_label(family: &str) -> &str {
    match family {
        "SML" => "Silver Maple Leaf",
        "Red Data Book" => "Libro Rojo de Rusia",
        "Serie de monedas de plata obtenidas a valor facial" => {
            "Monedas españolas de plata a valor facial"
        }
        "Lunar ounce" => "Rwanda Lunar Ounce",
        "Nautical Ounce" => "Rwanda Nautical Ounce",
        _ => family,
    }
}

fn is_technical_family(family: &str) -> bool {
    family.strip_prefix("System ").is_some_and(|period| {
        !period.is_empty()
            && period
                .split('-')
                .all(|year| year.len() == 4 && year.bytes().all(|byte| byte.is_ascii_digit()))
    })
}

fn finish_key(finish: Option<&Finish>) -> u8 {
    match finish {
        None => 0,
        Some(Finish::Bullion) => 1,
        Some(Finish::Proof) => 2,
        Some(Finish::Coloured) => 3,
        Some(Finish::ProofColoured) => 4,
        Some(Finish::Gilded) => 5,
        Some(Finish::Antiqued) => 6,
    }
}

fn finish_code(finish: Option<&Finish>) -> &'static str {
    match finish {
        None => "unknown",
        Some(Finish::Bullion) => "bullion",
        Some(Finish::Proof) => "proof",
        Some(Finish::Coloured) => "coloured",
        Some(Finish::ProofColoured) => "proof_coloured",
        Some(Finish::Gilded) => "gilded",
        Some(Finish::Antiqued) => "antiqued",
    }
}

fn finish_from_code(code: &str) -> Option<Option<Finish>> {
    match code {
        "unknown" => Some(None),
        "bullion" => Some(Some(Finish::Bullion)),
        "proof" => Some(Some(Finish::Proof)),
        "coloured" => Some(Some(Finish::Coloured)),
        "proof_coloured" => Some(Some(Finish::ProofColoured)),
        "gilded" => Some(Some(Finish::Gilded)),
        "antiqued" => Some(Some(Finish::Antiqued)),
        _ => None,
    }
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct ManualOverride {
    pub item_id: u64,
    pub slot_id: Option<SlotId>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub enum MatchSource {
    ManualOverride,
    ExplicitTypeId,
    Heuristic {
        confidence: f32,
        explanation: String,
    },
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct ItemRef {
    pub item_id: u64,
    pub type_id: u32,
    pub quantity: u32,
    pub match_source: Option<MatchSource>,
    #[serde(default)]
    pub unmatched_reason: Option<UnmatchedReason>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub enum UnmatchedReason {
    NoMatchingSlot,
    ExcludedByManualOverride,
    InvalidManualOverride {
        slot_id: SlotId,
        matching_slots: usize,
    },
    AmbiguousExplicitTypeId {
        slot_ids: Vec<SlotId>,
    },
    AmbiguousHeuristic {
        confidence: f32,
        slot_ids: Vec<SlotId>,
    },
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub enum SlotStatus {
    Owned { quantity: u32, items: Vec<ItemRef> },
    Missing,
    NotYetIssued,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct AlbumSlot {
    pub slot: Slot,
    pub status: SlotStatus,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct SeriesAlbum {
    pub series_id: SeriesId,
    pub name: String,
    pub slots: Vec<AlbumSlot>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct Album {
    pub series: Vec<SeriesAlbum>,
    pub unmatched: Vec<ItemRef>,
}

pub fn build_album(
    series: &[Series],
    items: &[CollectedItem],
    type_meta: &TypeMetaIndex,
    overrides: &[ManualOverride],
) -> Album {
    let mut album = Album {
        series: series
            .iter()
            .map(|series| SeriesAlbum {
                series_id: series.id.clone(),
                name: series.name.clone(),
                slots: series
                    .slots
                    .iter()
                    .cloned()
                    .map(|slot| {
                        let status = match slot.release_status {
                            ReleaseStatus::Issued => SlotStatus::Missing,
                            ReleaseStatus::Announced | ReleaseStatus::Expected => {
                                SlotStatus::NotYetIssued
                            }
                        };
                        AlbumSlot { slot, status }
                    })
                    .collect(),
            })
            .collect(),
        unmatched: Vec::new(),
    };

    for item in items {
        if let Some(manual_override) = overrides
            .iter()
            .find(|manual_override| manual_override.item_id == item.id)
        {
            let Some(slot_id) = manual_override.slot_id.as_ref() else {
                album.unmatched.push(unmatched_item_ref(
                    item,
                    UnmatchedReason::ExcludedByManualOverride,
                ));
                continue;
            };
            let locations = slot_locations(&album, |slot| &slot.id == slot_id);
            if locations.len() == 1 {
                assign_item(
                    album_slot_mut(&mut album, locations[0]),
                    item,
                    MatchSource::ManualOverride,
                );
            } else {
                album.unmatched.push(unmatched_item_ref(
                    item,
                    UnmatchedReason::InvalidManualOverride {
                        slot_id: slot_id.clone(),
                        matching_slots: locations.len(),
                    },
                ));
            }
            continue;
        }

        let explicit_locations =
            slot_locations(&album, |slot| slot.numista_type_ids.contains(&item.type_id));
        if explicit_locations.len() == 1 {
            assign_item(
                album_slot_mut(&mut album, explicit_locations[0]),
                item,
                MatchSource::ExplicitTypeId,
            );
            continue;
        }
        if explicit_locations.len() > 1 {
            let slot_ids = explicit_locations
                .iter()
                .map(|location| album_slot(&album, *location).slot.id.clone())
                .collect();
            album.unmatched.push(unmatched_item_ref(
                item,
                UnmatchedReason::AmbiguousExplicitTypeId { slot_ids },
            ));
            continue;
        }

        let metadata = type_meta.get(&item.type_id);
        let mut heuristic_candidates: Vec<HeuristicCandidate> = album
            .series
            .iter()
            .enumerate()
            .flat_map(|(series_index, series)| {
                series
                    .slots
                    .iter()
                    .enumerate()
                    .filter_map(move |(slot_index, album_slot)| {
                        album_slot
                            .slot
                            .matchers
                            .iter()
                            .filter(|matcher| matcher.matches(item, metadata))
                            .max_by(|left, right| left.confidence.total_cmp(&right.confidence))
                            .map(|matcher| HeuristicCandidate {
                                location: (series_index, slot_index),
                                slot_id: album_slot.slot.id.clone(),
                                confidence: matcher.confidence,
                                explanation: matcher.explanation.clone(),
                            })
                    })
            })
            .collect();
        heuristic_candidates.sort_by(|left, right| right.confidence.total_cmp(&left.confidence));

        if let Some(winner) = heuristic_candidates.first() {
            let top_confidence = winner.confidence;
            let tied_slot_ids: Vec<SlotId> = heuristic_candidates
                .iter()
                .take_while(|candidate| candidate.confidence == top_confidence)
                .map(|candidate| candidate.slot_id.clone())
                .collect();
            if tied_slot_ids.len() == 1 {
                let winner = heuristic_candidates.remove(0);
                assign_item(
                    album_slot_mut(&mut album, winner.location),
                    item,
                    MatchSource::Heuristic {
                        confidence: winner.confidence,
                        explanation: winner.explanation,
                    },
                );
            } else {
                album.unmatched.push(unmatched_item_ref(
                    item,
                    UnmatchedReason::AmbiguousHeuristic {
                        confidence: top_confidence,
                        slot_ids: tied_slot_ids,
                    },
                ));
            }
        } else {
            album
                .unmatched
                .push(unmatched_item_ref(item, UnmatchedReason::NoMatchingSlot));
        }
    }

    album
}

type SlotLocation = (usize, usize);

struct HeuristicCandidate {
    location: SlotLocation,
    slot_id: SlotId,
    confidence: f32,
    explanation: String,
}

fn slot_locations(album: &Album, predicate: impl Fn(&Slot) -> bool) -> Vec<SlotLocation> {
    album
        .series
        .iter()
        .enumerate()
        .flat_map(|(series_index, series)| {
            series.slots.iter().enumerate().filter_map({
                let predicate = &predicate;
                move |(slot_index, album_slot)| {
                    predicate(&album_slot.slot).then_some((series_index, slot_index))
                }
            })
        })
        .collect()
}

fn album_slot(album: &Album, location: SlotLocation) -> &AlbumSlot {
    &album.series[location.0].slots[location.1]
}

fn album_slot_mut(album: &mut Album, location: SlotLocation) -> &mut AlbumSlot {
    &mut album.series[location.0].slots[location.1]
}

fn unmatched_item_ref(item: &CollectedItem, reason: UnmatchedReason) -> ItemRef {
    ItemRef {
        item_id: item.id,
        type_id: item.type_id,
        quantity: item.quantity,
        match_source: None,
        unmatched_reason: Some(reason),
    }
}

fn assign_item(album_slot: &mut AlbumSlot, item: &CollectedItem, match_source: MatchSource) {
    let item_ref = ItemRef {
        item_id: item.id,
        type_id: item.type_id,
        quantity: item.quantity,
        match_source: Some(match_source),
        unmatched_reason: None,
    };
    match &mut album_slot.status {
        SlotStatus::Owned { quantity, items } => {
            *quantity = quantity.saturating_add(item.quantity);
            items.push(item_ref);
        }
        SlotStatus::Missing | SlotStatus::NotYetIssued => {
            album_slot.status = SlotStatus::Owned {
                quantity: item.quantity,
                items: vec![item_ref],
            };
        }
    }
}

impl Matcher {
    fn matches(&self, item: &CollectedItem, metadata: Option<&TypeMeta>) -> bool {
        let issuer_code = item
            .issuer_code
            .as_deref()
            .or_else(|| metadata.and_then(|value| value.issuer_code.as_deref()));
        let year = item
            .gregorian_year
            .or(item.issue_year)
            .or_else(|| metadata.and_then(|value| value.min_year));
        let title = item
            .title
            .as_deref()
            .or_else(|| metadata.and_then(|value| value.title.as_deref()));

        self.issuer_code
            .as_deref()
            .is_none_or(|expected| issuer_code == Some(expected))
            && self.year.is_none_or(|expected| year == Some(expected))
            && self.weight_oz.is_none_or(|expected| {
                metadata
                    .and_then(|value| value.weight_oz)
                    .is_some_and(|actual| (actual - expected).abs() <= 0.01)
            })
            && self.finish.as_ref().is_none_or(|expected| {
                metadata
                    .and_then(|value| value.finish.as_ref())
                    .is_some_and(|actual| actual == expected)
            })
            && self.title_contains.iter().all(|needle| {
                title.is_some_and(|value| value.to_lowercase().contains(&needle.to_lowercase()))
            })
    }
}

#[derive(Clone, Debug, Error, PartialEq)]
pub enum SeriesValidationError {
    #[error("{kind} id `{value}` must be a nonempty lowercase slug")]
    InvalidId { kind: &'static str, value: String },
    #[error("slot id `{slot_id}` is duplicated")]
    DuplicateSlotId { slot_id: SlotId },
    #[error("required field `{field}` cannot be blank")]
    BlankField { field: String },
    #[error("slot `{slot_id}` has invalid weight `{weight_oz}` oz")]
    InvalidWeight { slot_id: SlotId, weight_oz: f32 },
    #[error("Numista type id `{type_id}` is assigned more than once in the series")]
    DuplicateNumistaTypeId { type_id: u32 },
    #[error("source key `{slot_id}` does not reference a slot in the series")]
    UnknownSourceSlot { slot_id: SlotId },
    #[error("matcher {matcher_index} in slot `{slot_id}` is invalid: {reason}")]
    InvalidMatcher {
        slot_id: SlotId,
        matcher_index: usize,
        reason: String,
    },
}

impl Series {
    pub fn validate(&self) -> Result<(), SeriesValidationError> {
        validate_id("series", self.id.as_str())?;
        validate_nonblank("series.name", &self.name)?;
        validate_nonblank("series.mint", &self.mint)?;
        validate_nonblank("series.issuer_code", &self.issuer_code)?;

        let mut slot_ids = BTreeSet::new();
        let mut type_ids = BTreeSet::new();
        for slot in &self.slots {
            validate_id("slot", slot.id.as_str())?;
            if !slot_ids.insert(&slot.id) {
                return Err(SeriesValidationError::DuplicateSlotId {
                    slot_id: slot.id.clone(),
                });
            }
            validate_nonblank(&format!("slot[{}].label", slot.id), &slot.label)?;
            validate_nonblank(&format!("slot[{}].motif", slot.id), &slot.motif)?;
            if !slot.weight_oz.is_finite() || slot.weight_oz <= 0.0 {
                return Err(SeriesValidationError::InvalidWeight {
                    slot_id: slot.id.clone(),
                    weight_oz: slot.weight_oz,
                });
            }
            for type_id in &slot.numista_type_ids {
                if !type_ids.insert(*type_id) {
                    return Err(SeriesValidationError::DuplicateNumistaTypeId {
                        type_id: *type_id,
                    });
                }
            }
            for (matcher_index, matcher) in slot.matchers.iter().enumerate() {
                validate_matcher(&slot.id, matcher_index, matcher)?;
            }
        }
        for source_slot_id in self.sources.keys() {
            if !slot_ids.contains(source_slot_id) {
                return Err(SeriesValidationError::UnknownSourceSlot {
                    slot_id: source_slot_id.clone(),
                });
            }
        }
        Ok(())
    }
}

#[derive(Clone, Debug, Error, PartialEq)]
pub enum CatalogValidationError {
    #[error("series id `{series_id}` is duplicated")]
    DuplicateSeriesId { series_id: SeriesId },
    #[error(
        "slot id `{slot_id}` is duplicated across series `{first_series_id}` and `{second_series_id}`"
    )]
    DuplicateSlotId {
        slot_id: SlotId,
        first_series_id: SeriesId,
        second_series_id: SeriesId,
    },
    #[error(
        "Numista type id `{type_id}` is assigned to slots `{first_slot_id}` and `{second_slot_id}`"
    )]
    DuplicateNumistaTypeId {
        type_id: u32,
        first_slot_id: SlotId,
        second_slot_id: SlotId,
    },
    #[error("series `{series_id}` is invalid: {source}")]
    InvalidSeries {
        series_id: SeriesId,
        #[source]
        source: SeriesValidationError,
    },
}

pub fn validate_catalog(series: &[Series]) -> Result<(), CatalogValidationError> {
    let mut series_ids = BTreeSet::new();
    let mut slot_owners: BTreeMap<&SlotId, &SeriesId> = BTreeMap::new();
    let mut type_owners: BTreeMap<u32, &SlotId> = BTreeMap::new();

    for definition in series {
        definition
            .validate()
            .map_err(|source| CatalogValidationError::InvalidSeries {
                series_id: definition.id.clone(),
                source,
            })?;
        if !series_ids.insert(&definition.id) {
            return Err(CatalogValidationError::DuplicateSeriesId {
                series_id: definition.id.clone(),
            });
        }

        for slot in &definition.slots {
            if let Some(first_series_id) = slot_owners.insert(&slot.id, &definition.id) {
                return Err(CatalogValidationError::DuplicateSlotId {
                    slot_id: slot.id.clone(),
                    first_series_id: first_series_id.clone(),
                    second_series_id: definition.id.clone(),
                });
            }
            for type_id in &slot.numista_type_ids {
                if let Some(first_slot_id) = type_owners.insert(*type_id, &slot.id) {
                    return Err(CatalogValidationError::DuplicateNumistaTypeId {
                        type_id: *type_id,
                        first_slot_id: first_slot_id.clone(),
                        second_slot_id: slot.id.clone(),
                    });
                }
            }
        }
    }

    Ok(())
}

fn validate_id(kind: &'static str, value: &str) -> Result<(), SeriesValidationError> {
    if is_slug(value) {
        Ok(())
    } else {
        Err(SeriesValidationError::InvalidId {
            kind,
            value: value.into(),
        })
    }
}

fn is_slug(value: &str) -> bool {
    !value.is_empty()
        && value.split('-').all(|segment| {
            !segment.is_empty()
                && segment
                    .chars()
                    .all(|character| character.is_ascii_lowercase() || character.is_ascii_digit())
        })
}

fn validate_nonblank(field: &str, value: &str) -> Result<(), SeriesValidationError> {
    if value.trim().is_empty() {
        Err(SeriesValidationError::BlankField {
            field: field.into(),
        })
    } else {
        Ok(())
    }
}

fn validate_matcher(
    slot_id: &SlotId,
    matcher_index: usize,
    matcher: &Matcher,
) -> Result<(), SeriesValidationError> {
    let invalid = |reason: &str| SeriesValidationError::InvalidMatcher {
        slot_id: slot_id.clone(),
        matcher_index,
        reason: reason.into(),
    };

    if !matcher.confidence.is_finite() || !(0.0..=1.0).contains(&matcher.confidence) {
        return Err(invalid("confidence must be between 0 and 1"));
    }
    if matcher.explanation.trim().is_empty() {
        return Err(invalid("explanation cannot be blank"));
    }
    if matcher
        .issuer_code
        .as_ref()
        .is_some_and(|value| value.trim().is_empty())
        || matcher
            .title_contains
            .iter()
            .any(|value| value.trim().is_empty())
    {
        return Err(invalid("text criteria cannot be blank"));
    }
    if matcher
        .weight_oz
        .is_some_and(|value| !value.is_finite() || value <= 0.0)
    {
        return Err(invalid("weight must be finite and greater than zero"));
    }
    if matcher.issuer_code.is_none()
        && matcher.year.is_none()
        && matcher.weight_oz.is_none()
        && matcher.finish.is_none()
        && matcher.title_contains.is_empty()
    {
        return Err(invalid("at least one matching criterion is required"));
    }

    Ok(())
}
