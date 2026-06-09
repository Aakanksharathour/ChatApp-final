import { getInitials, getAvatarColor } from '../data/mockData'

/**
 * @param {{ name: string, size?: 36|40|48|80|96, online?: boolean, style?: object }} props
 */
export default function Avatar({ name = '', size = 48, online = false, style = {} }) {
  const cls     = `av av-${size}`
  const bg      = getAvatarColor(name)
  const initials = getInitials(name)

  return (
    <div className={cls} style={{ background: bg, ...style }}>
      {initials}
      {online && <span className="online-dot" />}
    </div>
  )
}
