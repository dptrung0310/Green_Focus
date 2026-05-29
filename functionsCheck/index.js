const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Helper function to send push notifications to all registered devices of a user.
 * Automatically cleans up invalid/expired FCM registration tokens from Firestore.
 * 
 * @param {string} toUid - The ID of the recipient user.
 * @param {object} payload - The FCM message payload including notification and data properties.
 */
async function sendToUserDevices(toUid, payload) {
  try {
    // 1. Fetch all active FCM tokens for the recipient user
    const tokensSnapshot = await db
      .collection("users")
      .document(toUid)
      .collection("fcmTokens")
      .get();

    if (tokensSnapshot.empty) {
      console.log(`No registered FCM tokens found for user: ${toUid}`);
      return;
    }

    const tokens = [];
    tokensSnapshot.forEach((doc) => {
      const data = doc.data();
      if (data.token) {
        tokens.push(data.token);
      }
    });

    if (tokens.length === 0) {
      console.log(`Token array is empty for user: ${toUid}`);
      return;
    }

    // 2. Construct the multicast FCM message
    // NOTE: Includes BOTH notification and data payload as requested
    const message = {
      tokens: tokens,
      notification: {
        title: payload.notification.title,
        body: payload.notification.body,
      },
      data: payload.data,
      android: {
        priority: "high",
        notification: {
          sound: "default",
          defaultSound: true,
          defaultVibrateTimings: true,
        },
      },
    };

    // 3. Send the notification to all devices
    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`Successfully sent multicast message to user ${toUid}:`, response);

    // 4. Inspect errors to identify and remove invalid registration tokens
    const tokensToDelete = [];
    response.responses.forEach((resp, idx) => {
      if (!resp.success) {
        const error = resp.error;
        console.error(`FCM token failed: ${tokens[idx]}`, error);
        
        // Stale or invalid token codes
        if (
          error.code === "messaging/registration-token-not-registered" ||
          error.code === "messaging/invalid-registration-token"
        ) {
          tokensToDelete.push(tokens[idx]);
        }
      }
    });

    // 5. Delete invalid tokens from Firestore in a batch
    if (tokensToDelete.length > 0) {
      console.log(`Cleaning up ${tokensToDelete.length} invalid tokens for user: ${toUid}`);
      const batch = db.batch();
      tokensToDelete.forEach((token) => {
        const docRef = db
          .collection("users")
          .document(toUid)
          .collection("fcmTokens")
          .document(token);
        batch.delete(docRef);
      });
      await batch.commit();
      console.log("Stale tokens successfully pruned from Firestore.");
    }
  } catch (error) {
    console.error(`Error sending push notification to user ${toUid}:`, error);
  }
}

/**
 * Triggered automatically when a new friend request document is created in Firestore.
 */
exports.onFriendRequestCreated = functions.firestore
  .document("friend_requests/{requestId}")
  .onCreate(async (snapshot, context) => {
    const requestData = snapshot.data();
    if (!requestData) {
      console.error("No friend request data found.");
      return null;
    }

    const { toUid, fromUid, fromDisplayName } = requestData;
    const requestId = snapshot.id;

    if (!toUid || !fromUid) {
      console.error("Missing required parameters: toUid or fromUid.");
      return null;
    }

    const payload = {
      notification: {
        title: "New friend request 🌱",
        body: `${fromDisplayName || "Someone"} sent you a friend request`,
      },
      data: {
        type: "friend_request",
        requestId: requestId,
        fromUid: fromUid,
      },
    };

    console.log(`Triggering FCM friend request notification for ${toUid}`);
    return sendToUserDevices(toUid, payload);
  });

/**
 * Triggered automatically when a new room invitation document is created in Firestore.
 */
exports.onRoomInviteCreated = functions.firestore
  .document("rooms/{roomId}/invites/{inviteId}")
  .onCreate(async (snapshot, context) => {
    const inviteData = snapshot.data();
    if (!inviteData) {
      console.error("No room invite data found.");
      return null;
    }

    const { toUid, fromUid, fromName } = inviteData;
    const roomId = context.params.roomId;
    const inviteId = snapshot.id;

    if (!toUid || !fromUid || !roomId) {
      console.error("Missing required parameters: toUid, fromUid, or roomId.");
      return null;
    }

    const payload = {
      notification: {
        title: "Room invitation 🌿",
        body: `${fromName || "Someone"} invited you to join a room`,
      },
      data: {
        type: "room_invite",
        inviteId: inviteId,
        roomId: roomId,
        fromUid: fromUid,
      },
    };

    console.log(`Triggering FCM room invite notification for user ${toUid} in room ${roomId}`);
    return sendToUserDevices(toUid, payload);
  });
