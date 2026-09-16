import React from 'react';
import { Popcorn, Ticket } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import MyTicketsView from './MyTicketsPage';
import FoodOrdersHistoryPage from './FoodOrdersHistoryPage';

export default function MyOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') === 'food' ? 'food' : 'tickets';

  const selectTab = (tab) => {
    const next = new URLSearchParams(searchParams);
    if (tab === 'food') next.set('tab', 'food');
    else next.delete('tab');
    setSearchParams(next, { replace: true });
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <header className="border-b border-white/10 pb-7">
        <p className="text-[10px] font-black uppercase tracking-[0.24em] text-amber-300">CinePremier Account</p>
        <h1 className="mt-3 text-1xl font-black uppercase tracking-tight text-white sm:text-3xl">Đơn của tôi</h1>
        <p className="mt-3 text-xs leading-5 text-neutral-400">Theo dõi vé xem phim và đơn bắp nước trong cùng một nơi.</p>
      </header>

      <div className="mb-6 grid border-b border-white/10 sm:grid-cols-2" role="tablist" aria-label="Loại đơn">
        <button type="button" role="tab" aria-selected={activeTab === 'tickets'} onClick={() => selectTab('tickets')} className={`flex min-h-10 items-center justify-center gap-2 border-b-2 px-4 py-2 text-[10px] font-black uppercase tracking-[0.16em] transition-colors ${activeTab === 'tickets' ? 'border-amber-300 bg-amber-300/[0.06] text-amber-200' : 'border-transparent text-neutral-500 hover:text-white'}`}><Ticket className="h-3.5 w-3.5" /> Vé xem phim</button>
        <button type="button" role="tab" aria-selected={activeTab === 'food'} onClick={() => selectTab('food')} className={`flex min-h-10 items-center justify-center gap-2 border-b-2 px-4 py-2 text-[10px] font-black uppercase tracking-[0.16em] transition-colors ${activeTab === 'food' ? 'border-purple-400 bg-purple-400/[0.06] text-purple-300' : 'border-transparent text-neutral-500 hover:text-white'}`}><Popcorn className="h-3.5 w-3.5" /> Bắp nước</button>
      </div>

      {activeTab === 'tickets' ? <MyTicketsView embedded /> : <FoodOrdersHistoryPage />}
    </div>
  );
}
