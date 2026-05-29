import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

interface NotificationPayload {
  notification: {
    title: string;
    body: string;
  };
  data: {[key: string]: string};
}

interface FcmTokenData {
  token?: string;
}

/**
 * Send push notifications to all registered devices of a user.
 * @param {string} toUid - The ID of the recipient user.
 * @param {NotificationPayload} payload - The FCM message payload.
 */
async function sendToUserDevices(
  toUid: string,
  payload: NotificationPayload
): Promise<void> {
  try {
    const tokensSnapshot = await db
      .collection("users")
      .doc(toUid)
      .collection("fcmTokens")
      .get();

    if (tokensSnapshot.empty) {
      console.log("No registered FCM tokens found for user:", toUid);
      return;
    }

    const tokens: string[] = [];
    tokensSnapshot.forEach((doc) => {
      const data = doc.data() as FcmTokenData;
      if (data.token) {
        tokens.push(data.token);
      }
    });

    if (tokens.length === 0) {
      console.log("Token array is empty for user:", toUid);
      return;
    }

    const message: admin.messaging.MulticastMessage = {
      tokens,
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

    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(
      `Successfully sent multicast message to user ${toUid}:`,
      response
    );

    const tokensToDelete: string[] = [];
    response.responses.forEach((resp, idx) => {
      if (!resp.success) {
        const error = resp.error;
        console.error(`FCM token failed: ${tokens[idx]}`, error);
        if (
          error &&
          (error.code === "messaging/registration-token-not-registered" ||
            error.code === "messaging/invalid-registration-token")
        ) {
          tokensToDelete.push(tokens[idx]);
        }
      }
    });

    if (tokensToDelete.length > 0) {
      console.log(
        "Cleaning up invalid tokens for user:",
        toUid,
        tokensToDelete.length
      );
      const batch = db.batch();
      tokensToDelete.forEach((token) => {
        const docRef = db
          .collection("users")
          .doc(toUid)
          .collection("fcmTokens")
          .doc(token);
        batch.delete(docRef);
      });
      await batch.commit();
      console.log("Stale tokens successfully pruned from Firestore.");
    }
  } catch (error) {
    console.error(
      `Error sending push notification to user ${toUid}:`,
      error
    );
  }
}

/** Trigger when a new friend request document is created. */
export const onFriendRequestCreated = functions.firestore
  .document("friend_requests/{requestId}")
  .onCreate(async (snapshot) => {
    const requestData = snapshot.data();
    if (!requestData) {
      console.error("No friend request data found.");
      return null;
    }
    const {toUid, fromUid, fromDisplayName} = requestData;
    const requestId = snapshot.id;
    if (!toUid || !fromUid) {
      console.error("Missing required parameters: toUid or fromUid.");
      return null;
    }
    const fromName = fromDisplayName || "Someone";
    const payload: NotificationPayload = {
      notification: {
        title: "New friend request ðŸŒ±",
        body: `${fromName} sent you a friend request`,
      },
      data: {
        type: "friend_request",
        requestId,
        fromUid,
      },
    };
    console.log("Triggering FCM friend request notification for", toUid);
    return sendToUserDevices(toUid, payload);
  });

/** Trigger when a new room invitation document is created. */
export const onRoomInviteCreated = functions.firestore
  .document("rooms/{roomId}/invites/{inviteId}")
  .onCreate(async (snapshot, context) => {
    const inviteData = snapshot.data();
    if (!inviteData) {
      console.error("No room invite data found.");
      return null;
    }
    const {toUid, fromUid, fromName} = inviteData;
    const roomId = context.params.roomId;
    const inviteId = snapshot.id;
    if (!toUid || !fromUid || !roomId) {
      console.error(
        "Missing required parameters: toUid, fromUid, or roomId."
      );
      return null;
    }
    const displayName = fromName || "Someone";
    const payload: NotificationPayload = {
      notification: {
        title: "Room invitation ðŸŒ¿",
        body: `${displayName} invited you to join a room`,
      },
      data: {
        type: "room_invite",
        inviteId,
        roomId,
        fromUid,
      },
    };
    console.log(
      "Triggering FCM room invite notification for user",
      toUid,
      "in room",
      roomId
    );
    return sendToUserDevices(toUid, payload);
  });
