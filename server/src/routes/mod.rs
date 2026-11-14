use std::collections::HashMap;

use crate::models::Event;

pub type WsPush = Option<(i64, Event)>;
pub type FcmPush = Option<(String, HashMap<String, String>)>;

pub mod checkins;
pub mod events;
pub mod locations;
pub mod messages;
pub mod pair;
pub mod places;
pub mod web;
