package com.hyperfactions.importer.simpleclaims;

import com.hyperfactions.util.Logger;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * Reads SimpleClaims data from its SQLite database ({@code SimpleClaims.db}).
 *
 * <p>Uses reflection-based JDBC driver detection — the SQLite driver must be on the
 * classpath (typically from the SimpleClaims JAR itself).
 */
public class ScSqliteReader {

  private final Path dbPath;

  /** Creates a new reader for the given database path. */
  public ScSqliteReader(@NotNull Path dbPath) {
    this.dbPath = dbPath;
  }

  /**
   * Checks if the SQLite JDBC driver is available on the classpath.
   *
   * @return true if the driver can be loaded
   */
  public static boolean isDriverAvailable() {
    try {
      Class.forName("org.sqlite.JDBC");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Reads all parties from the database.
   *
   * @return list of parties with their members, overrides, and allies populated
   * @throws SQLException if a database error occurs
   */
  public List<ScParty> readParties() throws SQLException {
    List<ScParty> parties = new ArrayList<>();

    try (Connection conn = getConnection()) {
      // Read base party data
      Map<String, ScPartyBuilder> builders = new LinkedHashMap<>();
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery(
             "SELECT id, owner, name, description, color, " +
             "created_user_uuid, created_user_name, created_date, " +
             "modified_user_uuid, modified_user_name, modified_date FROM parties")) {
        while (rs.next()) {
          String id = rs.getString("id");
          builders.put(id, new ScPartyBuilder(
            id,
            rs.getString("owner"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("color"),
            new ScTracker(rs.getString("created_user_uuid"),
              rs.getString("created_user_name"), rs.getString("created_date")),
            new ScTracker(rs.getString("modified_user_uuid"),
              rs.getString("modified_user_name"), rs.getString("modified_date"))
          ));
        }
      }

      // Read members
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT party_id, member_uuid FROM party_members")) {
        while (rs.next()) {
          ScPartyBuilder builder = builders.get(rs.getString("party_id"));
          if (builder != null) {
            builder.members.add(rs.getString("member_uuid"));
          }
        }
      }

      // Read overrides
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT party_id, type, value_type, value FROM party_overrides")) {
        while (rs.next()) {
          ScPartyBuilder builder = builders.get(rs.getString("party_id"));
          if (builder != null) {
            builder.overrides.add(new ScOverride(
              rs.getString("type"),
              new ScOverrideValue(rs.getString("value_type"), rs.getString("value"))
            ));
          }
        }
      }

      // Read party allies
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT party_id, ally_party_id FROM party_allies")) {
        while (rs.next()) {
          ScPartyBuilder builder = builders.get(rs.getString("party_id"));
          if (builder != null) {
            builder.partyAllies.add(rs.getString("ally_party_id"));
          }
        }
      }

      // Read player allies
      try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT party_id, player_uuid FROM player_allies")) {
        while (rs.next()) {
          ScPartyBuilder builder = builders.get(rs.getString("party_id"));
          if (builder != null) {
            builder.playerAllies.add(rs.getString("player_uuid"));
          }
        }
      }

      // Build ScParty records
      for (ScPartyBuilder b : builders.values()) {
        parties.add(new ScParty(
          b.id, b.owner, b.name, b.description,
          b.members.isEmpty() ? null : List.copyOf(b.members),
          b.color,
          b.overrides.isEmpty() ? null : List.copyOf(b.overrides),
          b.createdTracker, b.modifiedTracker,
          b.partyAllies.isEmpty() ? null : List.copyOf(b.partyAllies),
          b.playerAllies.isEmpty() ? null : List.copyOf(b.playerAllies)
        ));
      }
    }

    return parties;
  }

  /**
   * Reads all claims from the database, organized by dimension.
   *
   * @return claims grouped by dimension
   * @throws SQLException if a database error occurs
   */
  public ScClaims readClaims() throws SQLException {
    Map<String, List<ScChunkInfo>> byDimension = new LinkedHashMap<>();

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(
           "SELECT dimension, chunkX, chunkZ, party_owner, " +
           "created_user_uuid, created_user_name, created_date FROM claims")) {
      while (rs.next()) {
        String dim = rs.getString("dimension");
        // SQLite uses correct chunkZ column name — no ChunkY quirk
        ScChunkInfo chunk = new ScChunkInfo(
          rs.getString("party_owner"),
          rs.getInt("chunkX"),
          rs.getInt("chunkZ"), // stored directly as chunkZ, not via getChunkZ()
          new ScTracker(rs.getString("created_user_uuid"),
            rs.getString("created_user_name"), rs.getString("created_date"))
        );
        byDimension.computeIfAbsent(dim, k -> new ArrayList<>()).add(chunk);
      }
    }

    List<ScDimension> dimensions = new ArrayList<>();
    for (Map.Entry<String, List<ScChunkInfo>> entry : byDimension.entrySet()) {
      dimensions.add(new ScDimension(entry.getKey(), entry.getValue()));
    }

    return new ScClaims(dimensions);
  }

  /**
   * Reads the name cache from the database.
   *
   * @return map of UUID string to player name
   * @throws SQLException if a database error occurs
   */
  public Map<String, String> readNameCache() throws SQLException {
    Map<String, String> cache = new HashMap<>();

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT uuid, name FROM name_cache")) {
      while (rs.next()) {
        cache.put(rs.getString("uuid"), rs.getString("name"));
      }
    }

    return cache;
  }

  private Connection getConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
  }

  /** Mutable builder for assembling ScParty from multiple queries. */
  private static class ScPartyBuilder {
    final String id;
    final String owner;
    final String name;
    final String description;
    final int color;
    final ScTracker createdTracker;
    final ScTracker modifiedTracker;
    final List<String> members = new ArrayList<>();
    final List<ScOverride> overrides = new ArrayList<>();
    final List<String> partyAllies = new ArrayList<>();
    final List<String> playerAllies = new ArrayList<>();

    ScPartyBuilder(String id, String owner, String name, String description,
                   int color, ScTracker createdTracker, ScTracker modifiedTracker) {
      this.id = id;
      this.owner = owner;
      this.name = name;
      this.description = description;
      this.color = color;
      this.createdTracker = createdTracker;
      this.modifiedTracker = modifiedTracker;
    }
  }
}
