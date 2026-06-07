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
          icon: "ic_greenfocus_noti",
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
    const fromName = fromDisplayName || "Ai đó";
    const payload: NotificationPayload = {
      notification: {
        title: "Lời Mời Kết Bạn Mới",
        body: `${fromName} đã gửi cho bạn lời mời kết bạn`,
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
    const displayName = fromName || "Ai đó";
    const payload: NotificationPayload = {
      notification: {
        title: "Lời Mời Tham Gia Phòng",
        body: `${displayName} đã mời bạn tham gia phòng`,
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

/** Save one deterministic dead session per member when a room fails. */
export const onRoomFocusLost = functions.firestore
  .document("rooms/{roomId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const roomId = context.params.roomId;
    const focusLostEventId = after.focusLostEventId as string | undefined;

    if (!focusLostEventId ||
      focusLostEventId === before.focusLostEventId) {
      return null;
    }

    const membersSnapshot = await db
      .collection("rooms")
      .doc(roomId)
      .collection("members")
      .get();

    if (membersSnapshot.empty) {
      console.log("No room members found for failed room:", roomId);
      return null;
    }

    const durationMs = Number(after.durationMs) || 25 * 60 * 1000;
    const durationMinutes = Math.max(1, Math.floor(durationMs / 60000));
    const treeId = typeof after.treeId === "string" ?
      after.treeId :
      "default_oak";
    const startTime = Date.now();
    const batch = db.batch();

    membersSnapshot.forEach((memberDoc) => {
      const memberData = memberDoc.data();
      const uid = typeof memberData.uid === "string" ?
        memberData.uid :
        memberDoc.id;
      const sessionRef = db
        .collection("sessions")
        .doc(uid)
        .collection("user_sessions")
        .doc(focusLostEventId);

      batch.set(sessionRef, {
        treeId,
        startTime,
        durationMinutes,
        status: "DEAD",
        isGroupSession: true,
        roomId,
      }, {merge: true});
    });

    await batch.commit();
    console.log(
      "Saved failed group sessions for room:",
      roomId,
      focusLostEventId
    );
    return null;
  });

/** Save one deterministic alive session per member when a room completes. */
export const onRoomCompleted = functions.firestore
  .document("rooms/{roomId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const roomId = context.params.roomId;
    const completedEventId = after.completedEventId as string | undefined;

    if (!completedEventId ||
      completedEventId === before.completedEventId) {
      return null;
    }

    const membersSnapshot = await db
      .collection("rooms")
      .doc(roomId)
      .collection("members")
      .get();

    if (membersSnapshot.empty) {
      console.log("No room members found for completed room:", roomId);
      return null;
    }

    const durationMs = Number(after.durationMs) || 25 * 60 * 1000;
    const durationMinutes = Math.max(1, Math.floor(durationMs / 60000));
    const treeId = typeof after.treeId === "string" ?
      after.treeId :
      "default_oak";
    const startTime = Date.now();
    const batch = db.batch();

    membersSnapshot.forEach((memberDoc) => {
      const memberData = memberDoc.data();
      const uid = typeof memberData.uid === "string" ?
        memberData.uid :
        memberDoc.id;
      const sessionRef = db
        .collection("sessions")
        .doc(uid)
        .collection("user_sessions")
        .doc(completedEventId);

      batch.set(sessionRef, {
        treeId,
        startTime,
        durationMinutes,
        status: "ALIVE",
        isGroupSession: true,
        roomId,
      }, {merge: true});
    });

    await batch.commit();
    console.log(
      "Saved completed group sessions for room:",
      roomId,
      completedEventId
    );
    return null;
  });
