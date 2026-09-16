import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle, Armchair, Check, ChevronRight, CircleOff, DoorOpen,
  Edit3, Loader2, Plus, RefreshCw, Save, ScreenShare, Trash2, X
} from 'lucide-react';
import { adminService } from '../../../services/adminService';

const EMPTY_ROOM = {
  name: '',
  roomType: 'STANDARD',
  rowCount: 8,
  columnCount: 12,
  status: 'ACTIVE'
};

const ROOM_TYPES = [
  ['STANDARD', 'Tiêu chuẩn'],
  ['TWO_D', 'Phòng 2D'],
  ['THREE_D', 'Phòng 3D'],
  ['IMAX', 'IMAX'],
  ['VIP', 'Phòng VIP']
];

const SEAT_TYPES = [
  ['NORMAL', 'Thường'],
  ['STANDARD', 'Tiêu chuẩn'],
  ['VIP', 'VIP'],
  ['COUPLE', 'Ghế đôi']
];

const SEAT_STATUSES = [
  ['AVAILABLE', 'Sẵn sàng'],
  ['MAINTENANCE', 'Đang bảo trì'],
  ['UNAVAILABLE', 'Ngừng sử dụng']
];

const roomTypeLabel = (value) => ROOM_TYPES.find(([key]) => key === value)?.[1] || value;
const seatTypeLabel = (value) => SEAT_TYPES.find(([key]) => key === value)?.[1] || value;
const countSeatNumbers = (value) => value.split(',').map((item) => item.trim()).filter(Boolean).length;

const groupSeatsForDisplay = (rowSeats) => {
  const groups = [];
  for (let index = 0; index < rowSeats.length; index += 1) {
    const seat = rowSeats[index];
    const nextSeat = rowSeats[index + 1];
    if (seat.seatType === 'COUPLE' && nextSeat?.seatType === 'COUPLE') {
      groups.push([seat, nextSeat]);
      index += 1;
    } else {
      groups.push([seat]);
    }
  }
  return groups;
};

const getAdminCoupleSeatGroup = (seat, allSeats, forceCouple = false) => {
  if (!seat || (seat.seatType !== 'COUPLE' && !forceCouple)) return seat ? [seat] : [];
  const rowSeats = allSeats
    .filter(candidate => candidate.rowLabel === seat.rowLabel && (forceCouple || candidate.seatType === 'COUPLE'))
    .sort((a, b) => (a.displayColumn - b.displayColumn) || (a.seatNumber - b.seatNumber));
  const seatIndex = rowSeats.findIndex(candidate => candidate.id === seat.id);
  if (seatIndex < 0) return [seat];
  const pairStart = seatIndex % 2 === 0 ? seatIndex : seatIndex - 1;
  return rowSeats.slice(pairStart, pairStart + 2);
};

const rowLabelFromIndex = (index) => {
  let value = index;
  let label = '';
  do {
    label = String.fromCharCode(65 + (value % 26)) + label;
    value = Math.floor(value / 26) - 1;
  } while (value >= 0);
  return label;
};

const buildLayoutRows = (room, seatType) => Array.from({ length: room?.rowCount || 0 }, (_, index) => ({
  rowLabel: rowLabelFromIndex(index),
  displayOrder: index + 1,
  startColumn: 1,
  seatType,
  seatNumbers: Array.from({ length: room?.columnCount || 0 }, (_, seatIndex) => seatIndex + 1).join(', ')
}));

const buildSeatNumbers = (count) => Array.from({ length: Math.max(0, Number(count) || 0) }, (_, seatIndex) => seatIndex + 1).join(', ');

const syncLayoutRowsToRoomSize = (rows, room, seatType) => {
  const rowCount = Math.max(0, Number(room?.rowCount) || 0);
  const columnCount = Math.max(0, Number(room?.columnCount) || 0);
  const sourceRows = rows?.length ? rows : buildLayoutRows(room, seatType);
  return Array.from({ length: rowCount }, (_, index) => {
    const displayOrder = index + 1;
    const existingRow = sourceRows.find((row) => Number(row.displayOrder) === displayOrder) || sourceRows[index];
    return {
      rowLabel: existingRow?.rowLabel || rowLabelFromIndex(index),
      displayOrder,
      startColumn: 1,
      seatType: existingRow?.seatType || seatType,
      seatNumbers: buildSeatNumbers(columnCount)
    };
  });
};

const buildPreviewSeatsFromLayoutRows = (rows) => rows
  .flatMap((row) => {
    const rowLabel = String(row.rowLabel || '').trim().toUpperCase();
    const displayOrder = Number(row.displayOrder) || 1;
    const startColumn = Number(row.startColumn) || 1;
    return String(row.seatNumbers || '')
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => Number.isInteger(value) && value > 0)
      .map((seatNumber, index) => ({
        id: `draft-${rowLabel}-${seatNumber}-${index}`,
        seatRowId: `draft-row-${displayOrder}`,
        rowLabel,
        displayOrder,
        seatNumber,
        displayColumn: startColumn + index,
        startColumn,
        seatType: row.seatType,
        status: 'AVAILABLE',
        isDraft: true
      }));
  })
  .sort((a, b) => a.displayOrder - b.displayOrder || a.displayColumn - b.displayColumn);

const buildLayoutRowsFromSeats = (currentSeats) => Object.values(currentSeats.reduce((groups, seat) => {
  if (!groups[seat.seatRowId]) {
    groups[seat.seatRowId] = {
      rowLabel: seat.rowLabel,
      displayOrder: seat.displayOrder,
      startColumn: seat.startColumn,
      seatType: seat.seatType,
      seatNumbers: []
    };
  }
  groups[seat.seatRowId].seatNumbers.push(seat.seatNumber);
  return groups;
}, {}))
  .sort((a, b) => a.displayOrder - b.displayOrder)
  .map((row) => ({ ...row, seatNumbers: row.seatNumbers.join(', ') }));

export default function AdminRoomsPanel({ ctx }) {
  const { getAdminToken, showToast, addAuditLog } = ctx;

  // Stable ref – ensures useEffect([]) always calls the latest getAdminToken
  const getTokenRef = React.useRef(getAdminToken);
  React.useEffect(() => { getTokenRef.current = getAdminToken; }, [getAdminToken]);

  const [rooms, setRooms] = useState([]);
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [roomForm, setRoomForm] = useState(EMPTY_ROOM);
  const [seats, setSeats] = useState([]);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [seatForm, setSeatForm] = useState({ seatType: 'NORMAL', status: 'AVAILABLE' });
  const [defaultSeatType, setDefaultSeatType] = useState('NORMAL');
  const [layoutRows, setLayoutRows] = useState([]);
  const [isSeatLayoutDraft, setIsSeatLayoutDraft] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);

  const selectedRoom = rooms.find((room) => room.id === selectedRoomId) || null;
  const visibleSeats = useMemo(
    () => (isSeatLayoutDraft ? buildPreviewSeatsFromLayoutRows(layoutRows) : seats),
    [isSeatLayoutDraft, layoutRows, seats]
  );
  const groupedSeats = useMemo(() => visibleSeats.reduce((groups, seat) => {
    (groups[seat.rowLabel] ||= []).push(seat);
    return groups;
  }, {}), [visibleSeats]);
  const selectedSeatGroup = useMemo(
    () => getAdminCoupleSeatGroup(selectedSeat, visibleSeats, seatForm.seatType === 'COUPLE'),
    [selectedSeat, visibleSeats, seatForm.seatType]
  );
  const selectedSeatGroupIds = useMemo(
    () => new Set(selectedSeatGroup.map(seat => seat.id)),
    [selectedSeatGroup]
  );
  const selectedSeatLabel = selectedSeatGroup.length > 1
    ? selectedSeatGroup.map(seat => `${seat.rowLabel}${seat.seatNumber}`).join(' - ')
    : selectedSeat
      ? `${selectedSeat.rowLabel}${selectedSeat.seatNumber}`
      : '';
  const layoutSeatCount = useMemo(
    () => layoutRows.reduce((total, row) => total + countSeatNumbers(row.seatNumbers || ''), 0),
    [layoutRows]
  );

  const syncRoomForm = (room) => {
    setRoomForm(room ? {
      name: room.name || '',
      roomType: room.roomType || 'STANDARD',
      rowCount: room.rowCount || 1,
      columnCount: room.columnCount || 1,
      status: room.status || 'ACTIVE'
    } : EMPTY_ROOM);
  };

  const loadSeats = async (roomId, room = selectedRoom) => {
    const token = getAdminToken();
    if (!token || !roomId) return;
    try {
      const data = await adminService.getAdminRoomSeats(token, roomId);
      setSeats(data || []);
      setLayoutRows(data?.length ? buildLayoutRowsFromSeats(data) : buildLayoutRows(room, defaultSeatType));
      setIsSeatLayoutDraft(false);
      setSelectedSeat(null);
    } catch (error) {
      showToast(error.message || 'Không thể tải sơ đồ ghế.');
    }
  };

  const loadRooms = async (preferredRoomId = selectedRoomId) => {
    const token = getTokenRef.current?.();
    if (!token) return;
    setIsLoading(true);
    try {
      const data = await adminService.getAdminRooms(token);
      setRooms(data || []);
      const nextId = (data || []).some((room) => room.id === preferredRoomId)
        ? preferredRoomId
        : data?.[0]?.id || null;
      setSelectedRoomId(nextId);
      syncRoomForm((data || []).find((room) => room.id === nextId) || null);
      if (nextId) await loadSeats(nextId, (data || []).find((room) => room.id === nextId));
      else {
        setSeats([]);
        setIsSeatLayoutDraft(false);
      }
    } catch (error) {
      showToast(error.message || 'Không thể tải danh sách phòng chiếu.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadRooms();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const selectRoom = async (room) => {
    setSelectedRoomId(room.id);
    syncRoomForm(room);
    await loadSeats(room.id, room);
  };

  const startCreateRoom = () => {
    setSelectedRoomId(null);
    syncRoomForm(null);
    setSeats([]);
    setLayoutRows([]);
    setIsSeatLayoutDraft(false);
    setSelectedSeat(null);
  };

  const requestConfirm = (title, description, action) => {
    setConfirmAction({ title, description, action });
  };

  const runConfirmedAction = async () => {
    const action = confirmAction?.action;
    setConfirmAction(null);
    if (!action) return;
    setIsSaving(true);
    try {
      await action();
    } finally {
      setIsSaving(false);
    }
  };

  const saveRoom = () => {
    const payload = {
      ...roomForm,
      name: roomForm.name.trim(),
      rowCount: Number(roomForm.rowCount),
      columnCount: Number(roomForm.columnCount)
    };
    if (!payload.name) {
      showToast('Vui lòng nhập tên phòng.');
      return;
    }

    requestConfirm(
      selectedRoom ? 'Lưu thay đổi phòng chiếu?' : 'Tạo phòng chiếu mới?',
      selectedRoom
        ? 'Thông tin phòng sẽ được cập nhật ngay sau khi xác nhận.'
        : `Phòng "${payload.name}" sẽ được thêm vào rạp.`,
      async () => {
        const token = getAdminToken();
        if (!token) return;
        try {
          const roomSizeChanged = selectedRoom
            && (Number(selectedRoom.rowCount) !== payload.rowCount || Number(selectedRoom.columnCount) !== payload.columnCount);
          const saved = selectedRoom
            ? await adminService.updateAdminRoom(token, selectedRoom.id, payload)
            : await adminService.createAdminRoom(token, payload);
          addAuditLog(selectedRoom ? 'Cập nhật phòng chiếu' : 'Tạo phòng chiếu', saved.name);
          showToast(selectedRoom ? 'Đã cập nhật phòng chiếu.' : 'Đã tạo phòng chiếu.');
          if (!roomSizeChanged) {
            await loadRooms(saved.id);
          }
          if (roomSizeChanged) {
            setRooms((current) => current.map((room) => (room.id === saved.id ? saved : room)));
            setSelectedRoomId(saved.id);
            syncRoomForm(saved);
            setLayoutRows((current) => syncLayoutRowsToRoomSize(current, saved, defaultSeatType));
            setIsSeatLayoutDraft(true);
            setSelectedSeat(null);
            showToast('Đã đồng bộ phần sắp xếp ghế theo kích thước phòng mới. Hãy lưu cách sắp xếp mới để thay sơ đồ ghế.');
          }
        } catch (error) {
          showToast(error.message || 'Không thể lưu phòng chiếu.');
        }
      }
    );
  };

  const changeRoomStatus = () => {
    if (!selectedRoom) return;
    const nextStatus = selectedRoom.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    requestConfirm(
      nextStatus === 'ACTIVE' ? 'Mở lại phòng chiếu?' : 'Tạm ngừng phòng chiếu?',
      nextStatus === 'ACTIVE'
        ? 'Phòng sẽ được đưa trở lại hoạt động.'
        : 'Phòng sẽ tạm ngừng hoạt động cho đến khi quản trị viên mở lại.',
      async () => {
        const token = getAdminToken();
        if (!token) return;
        try {
          const saved = await adminService.updateAdminRoomStatus(token, selectedRoom.id, nextStatus);
          addAuditLog('Đổi trạng thái phòng chiếu', `${saved.name}: ${saved.status}`);
          showToast(nextStatus === 'ACTIVE' ? 'Đã mở lại phòng chiếu.' : 'Đã tạm ngừng phòng chiếu.');
          await loadRooms(saved.id);
        } catch (error) {
          showToast(error.message || 'Không thể đổi trạng thái phòng chiếu.');
        }
      }
    );
  };

  const saveSeatLayout = () => {
    if (!selectedRoom) return;
    if (layoutRows.length === 0) {
      showToast('Vui lòng thêm ít nhất một hàng ghế trước khi tạo sơ đồ.');
      return;
    }

    const normalizedRows = layoutRows.map((row) => {
      const rawSeatNumbers = row.seatNumbers.split(',').map((value) => value.trim()).filter(Boolean);
      return {
        rowLabel: row.rowLabel.trim().toUpperCase(),
        displayOrder: Number(row.displayOrder),
        startColumn: Number(row.startColumn),
        seatType: row.seatType,
        rawSeatNumbers,
        seatNumbers: rawSeatNumbers.map(Number)
      };
    });
    if (normalizedRows.some((row) => !row.rowLabel || row.seatNumbers.length === 0)) {
      showToast('Mỗi hàng phải có tên hàng và ít nhất một số ghế hợp lệ.');
      return;
    }
    if (normalizedRows.some((row) => row.seatNumbers.some((value) => !Number.isInteger(value) || value < 1))) {
      showToast('Danh sách số ghế chỉ được chứa số nguyên dương, cách nhau bằng dấu phẩy.');
      return;
    }
    if (new Set(normalizedRows.map((row) => row.rowLabel)).size !== normalizedRows.length) {
      showToast('Tên hàng ghế không được trùng nhau.');
      return;
    }
    if (new Set(normalizedRows.map((row) => row.displayOrder)).size !== normalizedRows.length) {
      showToast('Thứ tự hiển thị của các hàng không được trùng nhau.');
      return;
    }
    if (normalizedRows.some((row) => new Set(row.seatNumbers).size !== row.seatNumbers.length)) {
      showToast('Số ghế trong cùng một hàng không được trùng nhau.');
      return;
    }
    if (normalizedRows.some((row) => row.seatType === 'COUPLE' && row.seatNumbers.length % 2 !== 0)) {
      showToast('Ghế đôi phải đi thành từng cặp. Hàng có số ghế lẻ không thể chọn ghế đôi.');
      return;
    }

    const replacing = seats.length > 0;
    requestConfirm(
      replacing ? 'Lưu sơ đồ ghế mới?' : 'Tạo sơ đồ ghế?',
      replacing
        ? 'Sơ đồ hiện tại sẽ được thay bằng cách sắp xếp ghế bạn vừa thiết lập.'
        : 'Các hàng và ghế bạn vừa thiết lập sẽ được tạo cho phòng chiếu này.',
      async () => {
        const token = getAdminToken();
        if (!token) return;
        try {
          const payload = {
            defaultSeatType,
            rows: normalizedRows.map(({ rawSeatNumbers, ...row }) => row)
          };
          const data = replacing
            ? await adminService.replaceAdminRoomSeats(token, selectedRoom.id, payload)
            : await adminService.createAdminRoomSeats(token, selectedRoom.id, payload);
          setSeats(data || []);
          setLayoutRows(buildLayoutRowsFromSeats(data || []));
          setIsSeatLayoutDraft(false);
          setSelectedSeat(null);
          await loadSeats(selectedRoom.id, selectedRoom);
          addAuditLog(replacing ? 'Lưu sơ đồ ghế mới' : 'Tạo sơ đồ ghế', selectedRoom.name);
          showToast(replacing ? 'Đã lưu sơ đồ ghế mới.' : 'Đã tạo sơ đồ ghế.');
        } catch (error) {
          showToast(error.message || 'Không thể lưu sơ đồ ghế.');
        }
      }
    );
  };

  const updateLayoutRow = (index, field, value) => {
    setIsSeatLayoutDraft(true);
    setSelectedSeat(null);
    setLayoutRows((current) => current.map((row, rowIndex) => rowIndex === index ? { ...row, [field]: value } : row));
  };

  const changeDefaultSeatType = (seatType) => {
    if (seatType === 'COUPLE' && layoutRows.some((row) => countSeatNumbers(row.seatNumbers) % 2 !== 0)) {
      showToast('Không thể chọn ghế đôi vì đang có hàng chứa số ghế lẻ.');
      return;
    }
    setDefaultSeatType(seatType);
    setIsSeatLayoutDraft(true);
    setSelectedSeat(null);
    setLayoutRows((current) => current.map((row) => ({ ...row, seatType })));
    showToast(`Đã áp dụng loại ghế ${seatTypeLabel(seatType)} cho tất cả các hàng.`);
  };

  const changeLayoutRowSeatType = (index, seatType) => {
    if (seatType === 'COUPLE' && countSeatNumbers(layoutRows[index].seatNumbers) % 2 !== 0) {
      showToast(`Hàng ${layoutRows[index].rowLabel} có số ghế lẻ nên không thể chọn ghế đôi.`);
      return;
    }
    updateLayoutRow(index, 'seatType', seatType);
  };

  const removeLayoutRow = (index) => {
    const row = layoutRows[index];
    requestConfirm(
      `Bỏ hàng ghế ${row?.rowLabel || ''}?`,
      'Hàng này sẽ được bỏ khỏi phần thiết lập. Sơ đồ đang hoạt động chỉ thay đổi sau khi bạn lưu.',
      async () => {
        setIsSeatLayoutDraft(true);
        setSelectedSeat(null);
        setLayoutRows((current) => current.filter((_, rowIndex) => rowIndex !== index));
      }
    );
  };

  const chooseSeat = (seat) => {
    setSelectedSeat(seat);
    setSeatForm({ seatType: seat.seatType, status: seat.status });
  };

  const changeSelectedSeatType = (seatType) => {
    const rowSeatCount = seats.filter((seat) => seat.rowLabel === selectedSeat?.rowLabel).length;
    if (seatType === 'COUPLE' && rowSeatCount % 2 !== 0) {
      showToast('Hàng này có số ghế lẻ nên không thể chuyển thành ghế đôi.');
      return;
    }
    setSeatForm({ ...seatForm, seatType });
  };

  const saveSeat = () => {
    if (!selectedSeat) return;
    const affectsCouple = selectedSeat.seatType === 'COUPLE' || seatForm.seatType === 'COUPLE';
    requestConfirm(
      affectsCouple ? `Lưu thay đổi cặp ghế tại ${selectedSeat.rowLabel}${selectedSeat.seatNumber}?` : `Lưu thay đổi ghế ${selectedSeat.rowLabel}${selectedSeat.seatNumber}?`,
      affectsCouple ? 'Ghế này và ghế liền kề trong cùng cặp sẽ được cập nhật cùng nhau.' : 'Loại ghế và tình trạng sử dụng sẽ được cập nhật.',
      async () => {
        const token = getAdminToken();
        if (!token) return;
        try {
          const saved = await adminService.updateAdminSeat(token, selectedSeat.id, seatForm);
          await loadSeats(selectedRoom.id, selectedRoom);
          addAuditLog('Cập nhật ghế', `${saved.rowLabel}${saved.seatNumber}`);
          showToast(affectsCouple ? 'Đã cập nhật cả cặp ghế đôi.' : 'Đã cập nhật ghế.');
        } catch (error) {
          showToast(error.message || 'Không thể cập nhật ghế.');
        }
      }
    );
  };

  const deactivateSeat = () => {
    if (!selectedSeat) return;
    const isCoupleSeat = selectedSeat.seatType === 'COUPLE';
    requestConfirm(
      isCoupleSeat ? `Ngừng sử dụng cặp ghế tại ${selectedSeat.rowLabel}${selectedSeat.seatNumber}?` : `Ngừng sử dụng ghế ${selectedSeat.rowLabel}${selectedSeat.seatNumber}?`,
      isCoupleSeat ? 'Cả hai ghế trong cặp sẽ không còn sẵn sàng để khách đặt.' : 'Ghế sẽ không còn sẵn sàng để khách đặt.',
      async () => {
        const token = getAdminToken();
        if (!token) return;
        try {
          const saved = await adminService.deactivateAdminSeat(token, selectedSeat.id);
          await loadSeats(selectedRoom.id, selectedRoom);
          addAuditLog('Ngừng sử dụng ghế', `${saved.rowLabel}${saved.seatNumber}`);
          showToast(isCoupleSeat ? 'Đã ngừng sử dụng cả cặp ghế đôi.' : 'Đã ngừng sử dụng ghế.');
        } catch (error) {
          showToast(error.message || 'Không thể ngừng sử dụng ghế.');
        }
      }
    );
  };

  const fieldClass = 'w-full border border-white/10 bg-black px-3 py-2.5 text-sm font-bold text-white outline-none transition focus:border-amber-500/60';

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-4 border border-white/10 bg-gradient-to-r from-[#100d04] to-black p-5 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-[11px] font-black uppercase tracking-[0.3em] text-amber-500">Quản lý rạp</p>
          <h2 className="mt-1 text-xs font-black uppercase tracking-[0.18em] text-neutral-200">Phòng chiếu & ghế</h2>
          <p className="mt-1 text-xs text-neutral-500">Tạo phòng, thiết lập sơ đồ và quản lý tình trạng từng ghế.</p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => loadRooms()} className="flex items-center gap-2 border border-white/10 px-4 py-2 text-xs font-black uppercase text-neutral-300 hover:border-amber-500/50 hover:text-white">
            <RefreshCw className="h-3.5 w-3.5" /> Làm mới
          </button>
          <button onClick={startCreateRoom} className="flex items-center gap-2 bg-amber-500 px-4 py-2 text-xs font-black uppercase text-black hover:bg-amber-400">
            <Plus className="h-3.5 w-3.5" /> Tạo phòng mới
          </button>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="border border-white/10 bg-black/60 p-3">
          <div className="mb-3 flex items-center justify-between px-1">
            <span className="text-xs font-black uppercase tracking-widest text-neutral-400">Danh sách phòng</span>
            <span className="font-mono text-xs text-amber-400">{rooms.length}</span>
          </div>
          <div className="space-y-2">
            {isLoading && <div className="flex justify-center py-8"><Loader2 className="h-5 w-5 animate-spin text-amber-500" /></div>}
            {!isLoading && rooms.length === 0 && <p className="border border-dashed border-white/10 p-5 text-center text-xs text-neutral-500">Chưa có phòng chiếu.</p>}
            {rooms.map((room) => (
              <button key={room.id} onClick={() => selectRoom(room)} className={`w-full border p-3 text-left transition ${selectedRoomId === room.id ? 'border-amber-500/50 bg-amber-500/10' : 'border-white/10 bg-white/[0.02] hover:border-white/25'}`}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="text-xs font-black uppercase text-white">{room.name}</p>
                    <p className="mt-1 text-xs font-semibold text-neutral-400">{roomTypeLabel(room.roomType)} · {room.rowCount} hàng × {room.columnCount} ghế</p>
                  </div>
                  <ChevronRight className="h-4 w-4 text-neutral-600" />
                </div>
                <span className={`mt-3 inline-flex items-center gap-1 text-[11px] font-black uppercase ${room.status === 'ACTIVE' ? 'text-emerald-400' : 'text-rose-400'}`}>
                  <span className={`h-1.5 w-1.5 rounded-full ${room.status === 'ACTIVE' ? 'bg-emerald-400' : 'bg-rose-400'}`} />
                  {room.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm ngừng'}
                </span>
              </button>
            ))}
          </div>
        </aside>

        <main className="space-y-5">
          <section className="border border-white/10 bg-black/60 p-5">
            <div className="mb-5 flex items-center justify-between gap-3 border-b border-white/10 pb-4">
              <div className="flex items-center gap-3">
                <DoorOpen className="h-5 w-5 text-amber-500" />
                <div>
                  <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">{selectedRoom ? 'Thông tin phòng chiếu' : 'Tạo phòng chiếu mới'}</h3>
                  <p className="text-xs font-semibold text-neutral-400">Tên phòng được tự động bỏ khoảng trắng thừa và không phân biệt hoa thường khi kiểm tra trùng.</p>
                </div>
              </div>
              {selectedRoom && (
                <button onClick={changeRoomStatus} disabled={isSaving} className={`flex items-center gap-2 border px-3 py-2 text-[11px] font-black uppercase ${selectedRoom.status === 'ACTIVE' ? 'border-rose-500/30 text-rose-400' : 'border-emerald-500/30 text-emerald-400'}`}>
                  <CircleOff className="h-3.5 w-3.5" /> {selectedRoom.status === 'ACTIVE' ? 'Tạm ngừng phòng' : 'Mở lại phòng'}
                </button>
              )}
            </div>
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
              <label className="space-y-1.5 xl:col-span-2">
                <span className="text-[11px] font-black uppercase tracking-wider text-neutral-400">Tên phòng</span>
                <input value={roomForm.name} onChange={(event) => setRoomForm({ ...roomForm, name: event.target.value })} className={fieldClass} placeholder="Ví dụ: Phòng A" />
              </label>
              <label className="space-y-1.5">
                <span className="text-[11px] font-black uppercase tracking-wider text-neutral-400">Loại phòng</span>
                <select value={roomForm.roomType} onChange={(event) => setRoomForm({ ...roomForm, roomType: event.target.value })} className={fieldClass}>
                  {ROOM_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </label>
              <label className="space-y-1.5">
                <span className="text-[11px] font-black uppercase tracking-wider text-neutral-400">Số hàng ghế</span>
                <input type="number" min="1" max="52" value={roomForm.rowCount} onChange={(event) => setRoomForm({ ...roomForm, rowCount: event.target.value })} className={fieldClass} />
              </label>
              <label className="space-y-1.5">
                <span className="text-[11px] font-black uppercase tracking-wider text-neutral-400">Ghế mỗi hàng</span>
                <input type="number" min="1" max="50" value={roomForm.columnCount} onChange={(event) => setRoomForm({ ...roomForm, columnCount: event.target.value })} className={fieldClass} />
              </label>
            </div>
            <div className="mt-5 flex justify-end">
              <button onClick={saveRoom} disabled={isSaving} className="flex items-center gap-2 bg-white px-5 py-2.5 text-xs font-black uppercase text-black hover:bg-amber-400 disabled:opacity-50">
                <Save className="h-3.5 w-3.5" /> {selectedRoom ? 'Lưu thay đổi' : 'Tạo phòng'}
              </button>
            </div>
          </section>

          {selectedRoom && (
            <section className="border border-white/10 bg-black/60 p-5">
              <div className="flex flex-col gap-4 border-b border-white/10 pb-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="flex items-center gap-3">
                  <Armchair className="h-5 w-5 text-amber-400" />
                  <div>
                    <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">Sơ đồ ghế · {selectedRoom.name}</h3>
                    <p className="text-xs font-semibold text-neutral-400">{layoutSeatCount} ghế đang được sắp xếp</p>
                  </div>
                </div>
                <div className="flex flex-wrap items-end gap-2">
                  <label className="space-y-1">
                    <span className="block text-[11px] font-black uppercase tracking-wider text-neutral-400">Chọn loại ghế cho các hàng</span>
                    <select value={defaultSeatType} onChange={(event) => changeDefaultSeatType(event.target.value)} className={`${fieldClass} min-w-48`}>
                      {SEAT_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  </label>
                  <button onClick={saveSeatLayout} disabled={isSaving} className="flex items-center gap-2 border border-amber-500/40 bg-amber-500/10 px-5 py-3 text-[11px] font-black uppercase text-amber-300 hover:bg-amber-500/20">
                    <ScreenShare className="h-4 w-4" /> {seats.length ? 'Lưu cách sắp xếp mới' : 'Tạo sơ đồ ghế'}
                  </button>
                </div>
              </div>

              <div className="mt-5 border border-white/[0.10] bg-white/[0.03] p-4">
                <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h4 className="text-xs font-black uppercase tracking-widest text-amber-400">Sắp xếp từng hàng ghế</h4>
                    <p className="mt-1 text-xs font-semibold text-neutral-400">Kiểm tra từng hàng trước khi lưu. Các số ghế được ngăn cách bằng dấu phẩy, ví dụ: 1, 2, 3, 5.</p>
                  </div>
                  <span className="font-mono text-xs font-bold text-neutral-400">{layoutRows.length}/{selectedRoom.rowCount} hàng</span>
                </div>

                {layoutRows.length === 0 ? (
                  <div className="border border-dashed border-white/10 p-5 text-center text-xs font-semibold text-neutral-400">
                    Chưa có hàng ghế. Hãy cập nhật số hàng trong phần thông tin phòng rồi lưu lại.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {layoutRows.map((row, index) => (
                      <div key={index} className="grid items-end gap-3 border border-white/[0.12] bg-white/[0.04] p-4 lg:grid-cols-[100px_110px_120px_160px_minmax(220px,1fr)_44px]">
                        <label className="space-y-1">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Tên hàng</span>
                          <input value={row.rowLabel} onChange={(event) => updateLayoutRow(index, 'rowLabel', event.target.value)} className={fieldClass} />
                        </label>
                        <label className="space-y-1">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Vị trí hàng</span>
                          <input type="number" min="1" max={selectedRoom.rowCount} value={row.displayOrder} onChange={(event) => updateLayoutRow(index, 'displayOrder', event.target.value)} className={fieldClass} />
                        </label>
                        <label className="space-y-1">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Khoảng trống đầu hàng</span>
                          <input type="number" min="1" max={selectedRoom.columnCount} value={row.startColumn} onChange={(event) => updateLayoutRow(index, 'startColumn', event.target.value)} className={fieldClass} />
                        </label>
                        <label className="space-y-1">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Loại ghế</span>
                          <select value={row.seatType} onChange={(event) => changeLayoutRowSeatType(index, event.target.value)} className={fieldClass}>
                            {SEAT_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                          </select>
                        </label>
                        <label className="space-y-1">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Các số ghế trong hàng</span>
                          <input value={row.seatNumbers} onChange={(event) => updateLayoutRow(index, 'seatNumbers', event.target.value)} className={fieldClass} placeholder="1, 2, 3, 4" />
                        </label>
                        <button
                          type="button"
                          title="Bỏ hàng ghế"
                          onClick={() => removeLayoutRow(index)}
                          className="flex h-[42px] items-center justify-center border border-rose-500/30 text-rose-400 hover:bg-rose-500/10"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {visibleSeats.length === 0 ? (
                <div className="my-8 border border-dashed border-white/10 p-8 text-center">
                  <Armchair className="mx-auto h-7 w-7 text-neutral-700" />
                  <p className="mt-3 text-xs font-bold text-neutral-400">Phòng chưa có sơ đồ ghế</p>
                  <p className="mt-1 text-xs font-semibold text-neutral-500">Hãy kiểm tra cách sắp xếp từng hàng rồi tạo sơ đồ ghế.</p>
                </div>
              ) : (
                <div className="grid gap-5 pt-5 xl:grid-cols-[minmax(0,1fr)_300px]">
                  <div className="relative min-h-[430px] min-w-0 overflow-hidden border border-white/[0.08] bg-[#050505] p-7 pb-20">
                    {isSeatLayoutDraft && (
                      <div className="absolute right-0 top-0 z-20 flex items-center gap-2 border border-r-0 border-t-0 border-amber-400/35 bg-black/75 px-3 py-1.5 text-[10px] font-black uppercase tracking-wider text-amber-300 shadow-[0_0_18px_rgba(251,191,36,0.12)] backdrop-blur">
                        <span className="h-1.5 w-1.5 rounded-full bg-amber-300 shadow-[0_0_10px_rgba(252,211,77,0.75)]" />
                        Chưa lưu sơ đồ mới
                      </div>
                    )}
                    <div className="pointer-events-none absolute left-1/2 top-12 h-72 w-[78%] -translate-x-1/2 bg-gradient-to-b from-amber-300/[0.08] via-amber-300/[0.02] to-transparent [clip-path:polygon(18%_0,82%_0,100%_100%,0_100%)]" />
                    <div className="relative mx-auto mb-14 max-w-2xl">
                      <div className="h-2 rounded-[50%] bg-white/80 shadow-[0_0_12px_3px_rgba(255,255,255,0.5),0_0_55px_18px_rgba(255,255,255,0.1)]" />
                      <div className="pt-3 text-center text-[11px] font-black uppercase tracking-[0.42em] text-neutral-300">Màn hình chiếu</div>
                    </div>
                    <div className="relative z-10 w-full overflow-x-auto overflow-y-hidden pb-4">
                      <div className="mx-auto min-w-max w-fit space-y-3 px-2">
                        {Object.entries(groupedSeats).map(([rowLabel, rowSeats]) => (
                          <div key={rowLabel} className="flex items-center gap-3">
                            <span className="w-6 text-center font-mono text-xs font-black text-neutral-400">{rowLabel}</span>
                            <div className="flex gap-2">
                              {groupSeatsForDisplay(rowSeats).map((seatGroup) => (
                                <div key={seatGroup[0].id} className={`flex ${seatGroup.length === 2 ? 'gap-0 rounded-md border border-pink-300 bg-pink-500/35 p-0.5 shadow-[0_0_16px_rgba(244,114,182,0.55),0_0_30px_rgba(236,72,153,0.22)]' : ''}`}>
                                  {seatGroup.map((seat) => (
                                    <button
                                      key={seat.id}
                                      title={`${seat.rowLabel}${seat.seatNumber} · ${seatTypeLabel(seat.seatType)}`}
                                      onClick={() => !isSeatLayoutDraft && chooseSeat(seat)}
                                      className={`h-8 w-8 text-[11px] font-black shadow-[0_4px_0_rgba(0,0,0,0.7)] transition ${isSeatLayoutDraft ? 'cursor-default' : 'hover:-translate-y-0.5'} ${seatGroup.length === 2 ? 'border-pink-200/80 bg-pink-500/65 text-white first:rounded-l last:rounded-r hover:bg-pink-400' : 'border'} ${selectedSeatGroupIds.has(seat.id)
                                        ? 'bg-white text-black'
                                        : seat.status === 'UNAVAILABLE'
                                          ? 'border-rose-500/30 bg-rose-500/10 text-rose-400'
                                          : seat.status === 'MAINTENANCE'
                                            ? 'border-amber-500/30 bg-amber-500/10 text-amber-400'
                                            : seat.seatType === 'VIP'
                                              ? 'border-purple-500/40 bg-purple-500/10 text-purple-300'
                                              : seat.seatType === 'COUPLE'
                                                ? 'text-white'
                                                : 'border-white/[0.18] bg-white/[0.05] text-neutral-200 hover:border-white/[0.35] hover:bg-white/[0.08]'
                                        }`}
                                    >
                                      {seat.seatNumber}
                                    </button>
                                  ))}
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div className="absolute bottom-5 right-5 z-20 flex items-center gap-2 border border-emerald-400/40 bg-emerald-400/10 px-3 py-2 text-[11px] font-black uppercase tracking-wider text-emerald-300 shadow-[0_0_18px_rgba(52,211,153,0.12)]">
                      <DoorOpen className="h-4 w-4" /> Cửa ra vào
                    </div>
                  </div>

                  <div className="border border-white/[0.12] bg-white/[0.04] p-5">
                    {selectedSeat ? (
                      <div className="space-y-4">
                        <div>
                          <p className="text-[11px] font-black uppercase tracking-widest text-neutral-400">Ghế đang chọn</p>
                          <p className="mt-1 text-xl font-black text-white">{selectedSeatLabel}</p>
                        </div>
                        <label className="block space-y-1.5">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Loại ghế</span>
                          <select value={seatForm.seatType} onChange={(event) => changeSelectedSeatType(event.target.value)} className={fieldClass}>
                            {SEAT_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                          </select>
                        </label>
                        <label className="block space-y-1.5">
                          <span className="text-[11px] font-black uppercase text-neutral-400">Tình trạng</span>
                          <select value={seatForm.status} onChange={(event) => setSeatForm({ ...seatForm, status: event.target.value })} className={fieldClass}>
                            {SEAT_STATUSES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                          </select>
                        </label>
                        <button onClick={saveSeat} className="flex w-full items-center justify-center gap-2 bg-white px-3 py-2.5 text-[11px] font-black uppercase text-black hover:bg-amber-400">
                          <Check className="h-3.5 w-3.5" /> Lưu thay đổi ghế
                        </button>
                        <button onClick={deactivateSeat} className="flex w-full items-center justify-center gap-2 border border-rose-500/30 px-3 py-2.5 text-[11px] font-black uppercase text-rose-400 hover:bg-rose-500/10">
                          <CircleOff className="h-3.5 w-3.5" /> Ngừng sử dụng ghế
                        </button>
                      </div>
                    ) : (
                      <div className="flex h-full min-h-52 flex-col items-center justify-center text-center">
                        <Edit3 className="h-6 w-6 text-neutral-700" />
                        <p className="mt-3 text-xs font-bold text-neutral-400">Chọn một ghế để chỉnh sửa</p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </section>
          )}
        </main>
      </div>

      {confirmAction && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md border border-amber-500/30 bg-[#090909] p-6 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div className="flex gap-3">
                <div className="border border-amber-500/30 bg-amber-500/10 p-2 text-amber-400"><AlertTriangle className="h-5 w-5" /></div>
                <div>
                  <h3 className="text-xs font-black uppercase tracking-[0.18em] text-white">{confirmAction.title}</h3>
                  <p className="mt-2 text-xs leading-5 text-neutral-400">{confirmAction.description}</p>
                </div>
              </div>
              <button onClick={() => setConfirmAction(null)} className="text-neutral-500 hover:text-white"><X className="h-4 w-4" /></button>
            </div>
            <div className="mt-6 flex justify-end gap-2">
              <button onClick={() => setConfirmAction(null)} className="border border-white/10 px-4 py-2 text-[11px] font-black uppercase text-neutral-400 hover:text-white">Quay lại</button>
              <button onClick={runConfirmedAction} className="bg-amber-500 px-4 py-2 text-[11px] font-black uppercase text-black hover:bg-amber-400">Xác nhận</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
