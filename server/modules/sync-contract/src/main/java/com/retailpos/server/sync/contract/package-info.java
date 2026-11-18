/**
 * Sync contract v1. Rules: never send JPA entities; changes inside a contract version are additive only
 * (new optional fields); breaking changes need CONTRACT_VERSION + 1.
 */
package com.retailpos.server.sync.contract;
