ALTER TABLE open_chat_room ADD COLUMN first_message_sent_at DATETIME(6) NULL;
ALTER TABLE roommate_chatting_room ADD COLUMN first_message_sent_at DATETIME(6) NULL;

UPDATE open_chat_room r SET r.first_message_sent_at = NOW()
WHERE EXISTS (SELECT 1 FROM open_chat_message m WHERE m.room_id = r.id AND m.type IN ('TEXT', 'IMAGE'));

UPDATE roommate_chatting_room r SET r.first_message_sent_at = NOW()
WHERE EXISTS (SELECT 1 FROM roommate_chatting_chat c WHERE c.roommate_chatting_room_id = r.id AND c.is_system = 0);
